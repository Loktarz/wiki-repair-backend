package com.repairshop.repair_ticket_system.service;

import com.repairshop.repair_ticket_system.dto.DevisLigneRequest;
import com.repairshop.repair_ticket_system.dto.DevisLigneResponse;
import com.repairshop.repair_ticket_system.dto.DevisRequest;
import com.repairshop.repair_ticket_system.dto.DevisResponse;
import com.repairshop.repair_ticket_system.entity.Devis;
import com.repairshop.repair_ticket_system.entity.DevisLigne;
import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.entity.User;
import com.repairshop.repair_ticket_system.repository.DevisLigneRepository;
import com.repairshop.repair_ticket_system.repository.DevisRepository;
import com.repairshop.repair_ticket_system.repository.TicketRepository;
import com.repairshop.repair_ticket_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DevisService {

    private final DevisRepository      devisRepository;
    private final DevisLigneRepository devisLigneRepository;
    private final TicketRepository     ticketRepository;
    private final UserRepository       userRepository;
    private final EmailService         emailService;

    // ─── Create Devis ──────────────────────────────────────────────────────────

    @Transactional
    public DevisResponse createDevis(DevisRequest request, String creatorEmail) {
        Ticket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + request.getTicketId()));

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Assign infoline agent to the ticket
        ticket.setInfoline(creator);
        ticketRepository.save(ticket);

        Devis devis = Devis.builder()
                .ticket(ticket)
                .mainDoeuvre(request.getMainDoeuvre())
                .notes(request.getNotes())
                .createdBy(creator)
                .build();

        devis = devisRepository.save(devis);

        // Save each line item
        List<DevisLigne> lignes = buildLignes(request.getLignes(), devis);
        devisLigneRepository.saveAll(lignes);
        devis.getLignes().addAll(lignes);

        // Recalculate totals and persist
        devis.recalculate();
        DevisResponse response = toResponse(devisRepository.save(devis));

        // Notify client by email with devis summary (async)
        emailService.sendDevisEmail(ticket, devis.getTotalPiecesHT(), devis.getTva(), devis.getMontantTotal());

        return response;
    }

    // ─── Update Devis ──────────────────────────────────────────────────────────

    @Transactional
    public DevisResponse updateDevis(Long id, DevisRequest request) {
        Devis devis = devisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Devis not found with id: " + id));

        devis.setMainDoeuvre(request.getMainDoeuvre());
        devis.setNotes(request.getNotes());

        // Replace all existing lignes
        devisLigneRepository.deleteAll(devis.getLignes());
        devis.getLignes().clear();

        List<DevisLigne> lignes = buildLignes(request.getLignes(), devis);
        devisLigneRepository.saveAll(lignes);
        devis.getLignes().addAll(lignes);

        devis.recalculate();
        return toResponse(devisRepository.save(devis));
    }

    // ─── Accept / Refuse individual ligne ─────────────────────────────────────

    @Transactional
    public DevisLigneResponse updateLigneAcceptance(Long ligneId, Boolean acceptee) {
        DevisLigne ligne = devisLigneRepository.findById(ligneId)
                .orElseThrow(() -> new RuntimeException("Ligne not found with id: " + ligneId));

        ligne.setAcceptee(acceptee);
        devisLigneRepository.save(ligne);

        // Recalculate parent devis totals
        Devis devis = ligne.getDevis();
        devis.recalculate();
        devisRepository.save(devis);

        return toLigneResponse(ligne);
    }

    // ─── Get all devis for a ticket ────────────────────────────────────────────

    public List<DevisResponse> getDevisByTicket(Long ticketId) {
        return devisRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
                .stream().map(this::toResponse).toList();
    }

    // ─── Get latest devis for a ticket ────────────────────────────────────────

    public DevisResponse getLatestDevis(Long ticketId) {
        return devisRepository.findTopByTicketIdOrderByCreatedAtDesc(ticketId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("No devis found for ticket id: " + ticketId));
    }

    // ─── Get devis by ID ──────────────────────────────────────────────────────

    public DevisResponse getDevisById(Long id) {
        return devisRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Devis not found with id: " + id));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<DevisLigne> buildLignes(List<DevisLigneRequest> requests, Devis devis) {
        if (requests == null) return new ArrayList<>();
        return requests.stream()
                .map(r -> DevisLigne.builder()
                        .devis(devis)
                        .description(r.getDescription())
                        .quantite(r.getQuantite())
                        .prixUnitaire(r.getPrixUnitaire())
                        .acceptee(null) // pending by default
                        .build())
                .toList();
    }

    // ─── Map Entity → DTO ─────────────────────────────────────────────────────

    private DevisResponse toResponse(Devis devis) {
        List<DevisLigneResponse> ligneResponses = devis.getLignes().stream()
                .map(this::toLigneResponse)
                .toList();

        return DevisResponse.builder()
                .id(devis.getId())
                .ticketId(devis.getTicket().getId())
                .lignes(ligneResponses)
                .mainDoeuvre(devis.getMainDoeuvre())
                .totalPiecesHT(devis.getTotalPiecesHT())
                .tva(devis.getTva())
                .montantTotal(devis.getMontantTotal())
                .notes(devis.getNotes())
                .createdById(devis.getCreatedBy() != null ? devis.getCreatedBy().getId() : null)
                .createdByName(devis.getCreatedBy() != null ? devis.getCreatedBy().getFullName() : null)
                .createdAt(devis.getCreatedAt())
                .updatedAt(devis.getUpdatedAt())
                .build();
    }

    private DevisLigneResponse toLigneResponse(DevisLigne ligne) {
        int qty   = ligne.getQuantite()     != null ? ligne.getQuantite()     : 0;
        double pu = ligne.getPrixUnitaire() != null ? ligne.getPrixUnitaire() : 0.0;
        return DevisLigneResponse.builder()
                .id(ligne.getId())
                .description(ligne.getDescription())
                .quantite(ligne.getQuantite())
                .prixUnitaire(ligne.getPrixUnitaire())
                .totalHT(qty * pu)
                .acceptee(ligne.getAcceptee())
                .build();
    }
}
