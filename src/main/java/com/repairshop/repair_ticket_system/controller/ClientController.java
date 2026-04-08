package com.repairshop.repair_ticket_system.controller;

import com.repairshop.repair_ticket_system.dto.PublicTrackResponse;
import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.repository.TicketRepository;
import com.repairshop.repair_ticket_system.repository.UserRepository;
import com.repairshop.repair_ticket_system.service.PublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class ClientController {

    private final TicketRepository ticketRepository;
    private final UserRepository   userRepository;
    private final PublicService    publicService;

    // GET /api/client/my-tickets — returns all tickets linked to the logged-in client's email
    @GetMapping("/my-tickets")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<PublicTrackResponse>> myTickets(Principal principal) {
        String email = principal.getName();
        List<Ticket> tickets = ticketRepository.findByClientEmailOrderByCreatedAtDesc(email);
        List<PublicTrackResponse> response = tickets.stream()
                .map(publicService::toPublicResponse)
                .toList();
        return ResponseEntity.ok(response);
    }
}
