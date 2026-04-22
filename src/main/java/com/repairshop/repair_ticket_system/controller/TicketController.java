package com.repairshop.repair_ticket_system.controller;

import com.repairshop.repair_ticket_system.dto.StatusHistoryResponse;
import com.repairshop.repair_ticket_system.dto.TicketRequest;
import com.repairshop.repair_ticket_system.dto.TicketResponse;
import com.repairshop.repair_ticket_system.dto.TicketStatusUpdateRequest;
import com.repairshop.repair_ticket_system.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TicketController {

    private final TicketService ticketService;

    // POST /api/tickets — agent magasin or admin creates a ticket
    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT_MAGASIN', 'ADMIN')")
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody TicketRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createTicket(request, principal.getName()));
    }

    // GET /api/tickets — all staff can see all tickets (paginated)
    // ?page=0&size=10&search=ahmed&status=EN_DIAGNOSTIC
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN', 'TECHNICIAN', 'INFOLINE')")
    public ResponseEntity<Page<TicketResponse>> getAllTickets(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String search,
            @RequestParam(required = false)    String status) {
        return ResponseEntity.ok(ticketService.getTicketsPaginated(page, size, search, status));
    }

    // GET /api/tickets/{id} — all authenticated staff can view a single ticket
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN', 'TECHNICIAN', 'INFOLINE')")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    // PATCH /api/tickets/{id}/status — each role can only set their allowed statuses
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN', 'TECHNICIAN', 'INFOLINE')")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody TicketStatusUpdateRequest request,
            Principal principal) {
        return ResponseEntity.ok(ticketService.updateStatus(id, request, principal.getName()));
    }

    // PATCH /api/tickets/{id} — update ticket fields (agent magasin edits client info, technician edits diagnostic)
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN', 'TECHNICIAN', 'INFOLINE')")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketRequest request) {
        return ResponseEntity.ok(ticketService.updateTicket(id, request));
    }

    // PATCH /api/tickets/{id}/assign-technician — admin or agent magasin assigns a technician
    @PatchMapping("/{id}/assign-technician")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN')")
    public ResponseEntity<TicketResponse> assignTechnician(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> body) {
        return ResponseEntity.ok(ticketService.assignTechnician(id, body.get("technicianId")));
    }

    // PATCH /api/tickets/{id}/assign-infoline — admin or agent magasin assigns an infoline agent
    @PatchMapping("/{id}/assign-infoline")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN')")
    public ResponseEntity<TicketResponse> assignInfoline(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> body) {
        return ResponseEntity.ok(ticketService.assignInfoline(id, body.get("infolineId")));
    }

    // GET /api/tickets/{id}/history — all staff can view status history
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN', 'TECHNICIAN', 'INFOLINE')")
    public ResponseEntity<List<StatusHistoryResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getStatusHistory(id));
    }

    // DELETE /api/tickets/{id} — admin only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
