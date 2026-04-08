package com.repairshop.repair_ticket_system.service;

import com.repairshop.repair_ticket_system.dto.StatusHistoryResponse;
import com.repairshop.repair_ticket_system.dto.TicketRequest;
import com.repairshop.repair_ticket_system.dto.TicketResponse;
import com.repairshop.repair_ticket_system.dto.TicketStatusUpdateRequest;
import com.repairshop.repair_ticket_system.entity.ClientType;
import com.repairshop.repair_ticket_system.entity.Role;
import com.repairshop.repair_ticket_system.entity.StatusHistory;
import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.entity.TicketStatus;
import com.repairshop.repair_ticket_system.entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import com.repairshop.repair_ticket_system.repository.StatusHistoryRepository;
import com.repairshop.repair_ticket_system.repository.TicketRepository;
import com.repairshop.repair_ticket_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ─── Create Ticket ─────────────────────────────────────────────────────────

    public TicketResponse createTicket(TicketRequest request, String creatorEmail) {
        // Load the agent magasin (or admin) creating the ticket
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Ticket ticket = Ticket.builder()
                .ticketNumber(generateTicketNumber())
                .productFamily(request.getProductFamily())
                .productType(request.getProductType())
                .brand(request.getBrand())
                .designation(request.getDesignation())
                .reference(request.getReference())
                .serialNumber(request.getSerialNumber())
                .problemDescription(request.getProblemDescription())
                .accessories(request.getAccessories())
                .machineState(request.getMachineState())
                .warrantyPieces(request.getWarrantyPieces())
                .warrantyLabor(request.getWarrantyLabor())
                .serviceType(request.getServiceType())
                // Client info stored as plain fields (external person, not a system user)
                .clientType(request.getClientType())
                .clientName(request.getClientName())
                .clientPhone(request.getClientPhone())
                .clientEmail(request.getClientEmail())
                .clientAddress(request.getClientAddress())
                .clientCompany(request.getClientCompany())
                .agentMagasin(creator)
                .diagnosticNotes(request.getDiagnosticNotes())
                .build();
        // status is auto-set to EN_ATTENTE_DEPOT by @PrePersist in Ticket entity

        return toResponse(ticketRepository.save(ticket));
    }

    // ─── Get All Tickets ────────────────────────────────────────────────────────

    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Get Ticket By ID ───────────────────────────────────────────────────────

    public TicketResponse getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));
        return toResponse(ticket);
    }

    // ─── Update Status ──────────────────────────────────────────────────────────

    public TicketResponse updateStatus(Long id, TicketStatusUpdateRequest request, String actorEmail) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));

        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateStatusTransition(actor, request.getStatus());

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(request.getStatus());
        ticketRepository.save(ticket);

        // Log the status change
        statusHistoryRepository.save(StatusHistory.builder()
                .ticket(ticket)
                .oldStatus(oldStatus)
                .newStatus(request.getStatus())
                .changedByName(actor.getFullName())
                .changedByRole(actor.getRole().name())
                .build());

        return toResponse(ticket);
    }

    // ─── Generate Ticket Number ────────────────────────────────────────────────

    private String generateTicketNumber() {
        int year = LocalDateTime.now().getYear();
        long count = ticketRepository.countByYear(year) + 1;
        return String.format("WR-%d-%06d", year, count);
    }

    // ─── Status Transition Validation ─────────────────────────────────────────

    private void validateStatusTransition(User actor, TicketStatus newStatus) {
        if (actor.getRole() == Role.ADMIN) return; // admin bypasses all checks

        Set<TicketStatus> agentMagasinAllowed = Set.of(
                TicketStatus.DEPOSE_MAGASIN,
                TicketStatus.FICHE_REPARATION_IMPRIMEE,
                TicketStatus.DIAGNOSTIC_EN_ATTENTE,
                TicketStatus.PRET_RETRAIT,
                TicketStatus.LIVRE_CLIENT
        );

        Set<TicketStatus> technicianAllowed = Set.of(
                TicketStatus.EN_DIAGNOSTIC,
                TicketStatus.DIAGNOSTIC_TERMINE,
                TicketStatus.TENTATIVE_REPARATION,
                TicketStatus.ATTENTE_PIECE,
                TicketStatus.PIECE_RECUE,
                TicketStatus.EN_REPARATION,
                TicketStatus.REPARATION_TERMINEE,
                TicketStatus.REPARATION_IMPOSSIBLE
        );

        Set<TicketStatus> infolineAllowed = Set.of(
                TicketStatus.DEVIS_EN_ATTENTE,
                TicketStatus.DEVIS_ENVOYE_CLIENT,
                TicketStatus.DEVIS_ACCEPTE,
                TicketStatus.DEVIS_REFUSE
        );

        boolean allowed = switch (actor.getRole()) {
            case AGENT_MAGASIN -> agentMagasinAllowed.contains(newStatus);
            case TECHNICIAN    -> technicianAllowed.contains(newStatus);
            case INFOLINE      -> infolineAllowed.contains(newStatus);
            default            -> false;
        };

        if (!allowed) {
            throw new RuntimeException(
                "Role " + actor.getRole() + " is not allowed to set status: " + newStatus
            );
        }
    }

    // ─── Assign Technician ─────────────────────────────────────────────────────

    public TicketResponse assignTechnician(Long ticketId, Long technicianId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));

        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + technicianId));

        if (technician.getRole() != Role.TECHNICIAN && technician.getRole() != Role.ADMIN) {
            throw new RuntimeException("User with id " + technicianId + " is not a technician");
        }

        ticket.setTechnician(technician);
        return toResponse(ticketRepository.save(ticket));
    }

    // ─── Assign Infoline ───────────────────────────────────────────────────────

    public TicketResponse assignInfoline(Long ticketId, Long infolineId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));

        User infoline = userRepository.findById(infolineId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + infolineId));

        if (infoline.getRole() != Role.INFOLINE && infoline.getRole() != Role.ADMIN) {
            throw new RuntimeException("User with id " + infolineId + " is not an infoline agent");
        }

        ticket.setInfoline(infoline);
        return toResponse(ticketRepository.save(ticket));
    }

    // ─── Update Ticket Fields ──────────────────────────────────────────────────

    public TicketResponse updateTicket(Long id, TicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));

        ticket.setProductFamily(request.getProductFamily());
        ticket.setProductType(request.getProductType());
        ticket.setBrand(request.getBrand());
        ticket.setDesignation(request.getDesignation());
        ticket.setReference(request.getReference());
        ticket.setSerialNumber(request.getSerialNumber());
        ticket.setProblemDescription(request.getProblemDescription());
        ticket.setAccessories(request.getAccessories());
        ticket.setMachineState(request.getMachineState());
        ticket.setWarrantyPieces(request.getWarrantyPieces());
        ticket.setWarrantyLabor(request.getWarrantyLabor());
        ticket.setServiceType(request.getServiceType());
        ticket.setClientName(request.getClientName());
        ticket.setClientPhone(request.getClientPhone());
        ticket.setClientEmail(request.getClientEmail());
        ticket.setClientAddress(request.getClientAddress());
        ticket.setClientCompany(request.getClientCompany());
        ticket.setDiagnosticNotes(request.getDiagnosticNotes());

        return toResponse(ticketRepository.save(ticket));
    }

    // ─── Get Status History ────────────────────────────────────────────────────

    public List<StatusHistoryResponse> getStatusHistory(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));
        return statusHistoryRepository.findByTicketOrderByChangedAtDesc(ticket)
                .stream()
                .map(h -> StatusHistoryResponse.builder()
                        .id(h.getId())
                        .oldStatus(h.getOldStatus() != null ? h.getOldStatus().name() : null)
                        .oldStatusLabel(h.getOldStatus() != null ? translateStatus(h.getOldStatus()) : null)
                        .newStatus(h.getNewStatus().name())
                        .newStatusLabel(translateStatus(h.getNewStatus()))
                        .changedByName(h.getChangedByName())
                        .changedByRole(translateRole(h.getChangedByRole()))
                        .changedAt(h.getChangedAt() != null ? h.getChangedAt().format(FMT) : null)
                        .build())
                .toList();
    }

    private String translateStatus(TicketStatus s) {
        return switch (s) {
            case EN_ATTENTE_DEPOT          -> "En attente de dépôt";
            case DEPOSE_MAGASIN            -> "Déposé en magasin";
            case FICHE_REPARATION_IMPRIMEE -> "Fiche imprimée";
            case DIAGNOSTIC_EN_ATTENTE     -> "Diagnostic en attente";
            case EN_DIAGNOSTIC             -> "En diagnostic";
            case DIAGNOSTIC_TERMINE        -> "Diagnostic terminé";
            case DEVIS_EN_ATTENTE          -> "Devis en attente";
            case DEVIS_ENVOYE_CLIENT       -> "Devis envoyé";
            case DEVIS_ACCEPTE             -> "Devis accepté";
            case DEVIS_REFUSE              -> "Devis refusé";
            case TENTATIVE_REPARATION      -> "Tentative de réparation";
            case ATTENTE_PIECE             -> "Attente de pièce";
            case PIECE_RECUE               -> "Pièce reçue";
            case EN_REPARATION             -> "En réparation";
            case REPARATION_TERMINEE       -> "Réparation terminée";
            case PRET_RETRAIT              -> "Prêt pour retrait";
            case LIVRE_CLIENT              -> "Livré au client";
            case REPARATION_IMPOSSIBLE     -> "Réparation impossible";
        };
    }

    private String translateRole(String role) {
        if (role == null) return "";
        return switch (role) {
            case "ADMIN"         -> "Admin";
            case "AGENT_MAGASIN" -> "Agent Magasin";
            case "TECHNICIAN"    -> "Technicien";
            case "INFOLINE"      -> "Infoline";
            default              -> role;
        };
    }

    // ─── Delete Ticket ──────────────────────────────────────────────────────────

    public void deleteTicket(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new RuntimeException("Ticket not found with id: " + id);
        }
        ticketRepository.deleteById(id);
    }

    // ─── Map Entity → DTO ──────────────────────────────────────────────────────

    private TicketResponse toResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .productFamily(ticket.getProductFamily())
                .productType(ticket.getProductType())
                .brand(ticket.getBrand())
                .designation(ticket.getDesignation())
                .reference(ticket.getReference())
                .serialNumber(ticket.getSerialNumber())
                .problemDescription(ticket.getProblemDescription())
                .accessories(ticket.getAccessories())
                .machineState(ticket.getMachineState())
                .warrantyPieces(ticket.getWarrantyPieces())
                .warrantyLabor(ticket.getWarrantyLabor())
                .serviceType(ticket.getServiceType())
                .status(ticket.getStatus())
                // Client info (external)
                .clientType(ticket.getClientType())
                .clientName(ticket.getClientName())
                .clientPhone(ticket.getClientPhone())
                .clientEmail(ticket.getClientEmail())
                .clientAddress(ticket.getClientAddress())
                .clientCompany(ticket.getClientCompany())
                // Diagnostic
                .diagnosticNotes(ticket.getDiagnosticNotes())
                // Agent Magasin info
                .agentMagasinId(ticket.getAgentMagasin() != null ? ticket.getAgentMagasin().getId() : null)
                .agentMagasinName(ticket.getAgentMagasin() != null ? ticket.getAgentMagasin().getFullName() : null)
                // Infoline info
                .infolineId(ticket.getInfoline() != null ? ticket.getInfoline().getId() : null)
                .infolineName(ticket.getInfoline() != null ? ticket.getInfoline().getFullName() : null)
                // Technician info
                .technicianId(ticket.getTechnician() != null ? ticket.getTechnician().getId() : null)
                .technicianName(ticket.getTechnician() != null ? ticket.getTechnician().getFullName() : null)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
