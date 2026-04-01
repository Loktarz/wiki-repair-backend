package com.repairshop.repair_ticket_system.controller;

import com.repairshop.repair_ticket_system.dto.TicketRequest;
import com.repairshop.repair_ticket_system.dto.TicketResponse;
import com.repairshop.repair_ticket_system.dto.TicketStatusUpdateRequest;
import com.repairshop.repair_ticket_system.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class TicketController {

    private final TicketService ticketService;

    // POST /api/tickets — clerk, admin, or client creates a ticket
    @PostMapping
    @PreAuthorize("hasAnyRole('CLERK', 'ADMIN', 'CLIENT')")
    public ResponseEntity<TicketResponse> createTicket(
            @RequestBody TicketRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createTicket(request, principal.getName()));
    }

    // GET /api/tickets — all staff can see all tickets (not clients)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK', 'TECHNICIAN', 'INFOLINE')")
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    // GET /api/tickets/my — client sees only their own tickets
    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<TicketResponse>> getMyTickets(Principal principal) {
        return ResponseEntity.ok(ticketService.getMyTickets(principal.getName()));
    }

    // GET /api/tickets/{id} — all authenticated users can view a single ticket
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    // PATCH /api/tickets/{id}/status — staff moves ticket through the workflow
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLERK', 'TECHNICIAN', 'INFOLINE')")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody TicketStatusUpdateRequest request) {
        return ResponseEntity.ok(ticketService.updateStatus(id, request));
    }

    // DELETE /api/tickets/{id} — admin only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
