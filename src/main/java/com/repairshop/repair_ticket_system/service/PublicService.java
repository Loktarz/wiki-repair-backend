package com.repairshop.repair_ticket_system.service;

import com.repairshop.repair_ticket_system.dto.PublicDemandeRequest;
import com.repairshop.repair_ticket_system.dto.PublicTrackResponse;
import java.util.List;
import com.repairshop.repair_ticket_system.dto.TicketResponse;
import com.repairshop.repair_ticket_system.entity.ClientType;
import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.entity.TicketStatus;
import com.repairshop.repair_ticket_system.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PublicService {

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ─── Track tickets by phone number ────────────────────────────────────────
    public List<PublicTrackResponse> trackByPhone(String phone) {
        List<com.repairshop.repair_ticket_system.entity.Ticket> tickets =
                ticketRepository.findByClientPhoneContaining(phone);
        if (tickets.isEmpty()) throw new RuntimeException("Aucun ticket trouvé pour ce numéro de téléphone.");
        return tickets.stream().map(t -> {
            String phase      = resolvePhase(t.getStatus());
            String phaseLabel = resolvePhaseLabel(phase);
            String statusLabel = resolveStatusLabel(t.getStatus());
            return PublicTrackResponse.builder()
                    .ticketNumber(t.getTicketNumber())
                    .clientName(maskName(t.getClientName()))
                    .productType(t.getProductType())
                    .brand(t.getBrand())
                    .serviceType(t.getServiceType())
                    .status(t.getStatus() != null ? t.getStatus().name() : null)
                    .phase(phase)
                    .phaseLabel(phaseLabel)
                    .statusLabel(statusLabel)
                    .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().format(FMT) : null)
                    .updatedAt(t.getUpdatedAt() != null ? t.getUpdatedAt().format(FMT) : null)
                    .build();
        }).toList();
    }

    // ─── Track ticket by number ────────────────────────────────────────────────
    public PublicTrackResponse trackTicket(String ticketNumber) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Ticket introuvable : " + ticketNumber));

        String phase      = resolvePhase(ticket.getStatus());
        String phaseLabel = resolvePhaseLabel(phase);
        String statusLabel = resolveStatusLabel(ticket.getStatus());

        // Mask client name: "Ahmed Ben Ali" → "Ahmed B***"
        String maskedName = maskName(ticket.getClientName());

        return PublicTrackResponse.builder()
                .ticketNumber(ticket.getTicketNumber())
                .clientName(maskedName)
                .productType(ticket.getProductType())
                .brand(ticket.getBrand())
                .serviceType(ticket.getServiceType())
                .status(ticket.getStatus() != null ? ticket.getStatus().name() : null)
                .phase(phase)
                .phaseLabel(phaseLabel)
                .statusLabel(statusLabel)
                .createdAt(ticket.getCreatedAt() != null ? ticket.getCreatedAt().format(FMT) : null)
                .updatedAt(ticket.getUpdatedAt() != null ? ticket.getUpdatedAt().format(FMT) : null)
                .build();
    }

    // ─── Submit public repair request ─────────────────────────────────────────
    public TicketResponse submitDemande(PublicDemandeRequest req) {
        // Build a TicketRequest and delegate to TicketService (no creator = online submission)
        Ticket ticket = Ticket.builder()
                .ticketNumber(generateTicketNumber())
                .clientType(req.getClientType() != null
                        ? ClientType.valueOf(req.getClientType()) : ClientType.PARTICULIER)
                .clientName(req.getClientName())
                .clientPhone(req.getClientPhone())
                .clientEmail(req.getClientEmail())
                .clientAddress(req.getClientAddress())
                .clientCompany(req.getClientCompany())
                .productFamily(req.getProductFamily())
                .productType(req.getProductType())
                .brand(req.getBrand())
                .designation(req.getDesignation())
                .serialNumber(req.getSerialNumber())
                .machineState(req.getMachineState())
                .accessories(req.getAccessories())
                .problemDescription(req.getProblemDescription())
                .serviceType(req.getServiceType())
                // No agent assigned — ticket is EN_ATTENTE_DEPOT by @PrePersist
                .build();

        Ticket saved = ticketRepository.save(ticket);
        return ticketService.getTicketById(saved.getId());
    }

    // ─── Ticket number generation (delegates to TicketService logic) ──────────
    private String generateTicketNumber() {
        int year = java.time.LocalDateTime.now().getYear();
        long count = ticketRepository.countByYear(year) + 1;
        return String.format("WR-%d-%06d", year, count);
    }

    // ─── Phase resolution ──────────────────────────────────────────────────────
    private String resolvePhase(TicketStatus status) {
        if (status == null) return "DEPOT";
        return switch (status) {
            case EN_ATTENTE_DEPOT, DEPOSE_MAGASIN, FICHE_REPARATION_IMPRIMEE -> "DEPOT";
            case DIAGNOSTIC_EN_ATTENTE, EN_DIAGNOSTIC, DIAGNOSTIC_TERMINE    -> "DIAGNOSTIC";
            case DEVIS_EN_ATTENTE, DEVIS_ENVOYE_CLIENT,
                 DEVIS_ACCEPTE, DEVIS_REFUSE                                  -> "DEVIS";
            case TENTATIVE_REPARATION, ATTENTE_PIECE, PIECE_RECUE,
                 EN_REPARATION, REPARATION_TERMINEE                           -> "REPARATION";
            case PRET_RETRAIT                                                  -> "PRET";
            case LIVRE_CLIENT                                                  -> "LIVRE";
            case REPARATION_IMPOSSIBLE                                         -> "IMPOSSIBLE";
        };
    }

    private String resolvePhaseLabel(String phase) {
        return switch (phase) {
            case "DEPOT"       -> "Dépôt enregistré";
            case "DIAGNOSTIC"  -> "En cours de diagnostic";
            case "DEVIS"       -> "Devis en cours";
            case "REPARATION"  -> "En cours de réparation";
            case "PRET"        -> "Prêt à être retiré";
            case "LIVRE"       -> "Livré au client";
            case "IMPOSSIBLE"  -> "Réparation impossible";
            default            -> "En attente";
        };
    }

    private String resolveStatusLabel(TicketStatus status) {
        if (status == null) return "En attente";
        return switch (status) {
            case EN_ATTENTE_DEPOT          -> "En attente de dépôt";
            case DEPOSE_MAGASIN            -> "Déposé en magasin";
            case FICHE_REPARATION_IMPRIMEE -> "Fiche de réparation imprimée";
            case DIAGNOSTIC_EN_ATTENTE     -> "Diagnostic en attente";
            case EN_DIAGNOSTIC             -> "Diagnostic en cours";
            case DIAGNOSTIC_TERMINE        -> "Diagnostic terminé";
            case DEVIS_EN_ATTENTE          -> "Devis en attente";
            case DEVIS_ENVOYE_CLIENT       -> "Devis envoyé au client";
            case DEVIS_ACCEPTE             -> "Devis accepté";
            case DEVIS_REFUSE              -> "Devis refusé";
            case TENTATIVE_REPARATION      -> "Tentative de réparation";
            case ATTENTE_PIECE             -> "En attente de pièce";
            case PIECE_RECUE               -> "Pièce reçue";
            case EN_REPARATION             -> "En cours de réparation";
            case REPARATION_TERMINEE       -> "Réparation terminée";
            case PRET_RETRAIT              -> "Prêt pour retrait";
            case LIVRE_CLIENT              -> "Livré au client";
            case REPARATION_IMPOSSIBLE     -> "Réparation impossible";
        };
    }

    // ─── Mask name for privacy ────────────────────────────────────────────────
    private String maskName(String name) {
        if (name == null || name.isBlank()) return "—";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].charAt(0) + "***";
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(" ").append(parts[i].charAt(0)).append("***");
        }
        return sb.toString();
    }
}
