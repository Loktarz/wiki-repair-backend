package com.repairshop.repair_ticket_system.service;

import com.repairshop.repair_ticket_system.dto.TicketRequest;
import com.repairshop.repair_ticket_system.dto.TicketResponse;
import com.repairshop.repair_ticket_system.dto.TicketStatusUpdateRequest;
import com.repairshop.repair_ticket_system.entity.Role;
import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.entity.User;
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

    // ─── Create Ticket ─────────────────────────────────────────────────────────

    public TicketResponse createTicket(TicketRequest request, String creatorEmail) {
        // Load whoever is logged in (client, clerk, or admin)
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User client = null;
        User clerk = null;

        if (creator.getRole() == Role.CLIENT) {
            // Client creating their own ticket online → they ARE the client, no clerk yet
            client = creator;
        } else {
            // Clerk or Admin creating on behalf of a walk-in client
            clerk = creator;
            if (request.getClientId() != null) {
                client = userRepository.findById(request.getClientId())
                        .orElseThrow(() -> new RuntimeException("Client not found with id: " + request.getClientId()));
            }
        }

        Ticket ticket = Ticket.builder()
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
                .clerk(clerk)
                .client(client)
                .build();
        // status is auto-set to EN_ATTENTE_DEPOT by @PrePersist in Ticket entity

        return toResponse(ticketRepository.save(ticket));
    }

    // ─── Get My Tickets (client sees only their own) ───────────────────────────

    public List<TicketResponse> getMyTickets(String clientEmail) {
        User client = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ticketRepository.findByClientId(client.getId())
                .stream()
                .map(this::toResponse)
                .toList();
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

    public TicketResponse updateStatus(Long id, TicketStatusUpdateRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));

        ticket.setStatus(request.getStatus());
        return toResponse(ticketRepository.save(ticket));
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
                // Client info
                .clientId(ticket.getClient() != null ? ticket.getClient().getId() : null)
                .clientName(ticket.getClient() != null ? ticket.getClient().getFullName() : null)
                .clientEmail(ticket.getClient() != null ? ticket.getClient().getEmail() : null)
                // Clerk info
                .clerkId(ticket.getClerk() != null ? ticket.getClerk().getId() : null)
                .clerkName(ticket.getClerk() != null ? ticket.getClerk().getFullName() : null)
                // Technician info
                .technicianId(ticket.getTechnician() != null ? ticket.getTechnician().getId() : null)
                .technicianName(ticket.getTechnician() != null ? ticket.getTechnician().getFullName() : null)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
