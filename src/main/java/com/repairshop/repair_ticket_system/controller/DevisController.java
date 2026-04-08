package com.repairshop.repair_ticket_system.controller;

import com.repairshop.repair_ticket_system.dto.DevisLigneResponse;
import com.repairshop.repair_ticket_system.dto.DevisRequest;
import com.repairshop.repair_ticket_system.dto.DevisResponse;
import com.repairshop.repair_ticket_system.service.DevisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/devis")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class DevisController {

    private final DevisService devisService;

    // POST /api/devis — infoline creates a devis for a ticket
    @PostMapping
    @PreAuthorize("hasAnyRole('INFOLINE', 'ADMIN')")
    public ResponseEntity<DevisResponse> createDevis(
            @RequestBody DevisRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(devisService.createDevis(request, principal.getName()));
    }

    // PUT /api/devis/{id} — infoline updates a devis
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INFOLINE', 'ADMIN')")
    public ResponseEntity<DevisResponse> updateDevis(
            @PathVariable Long id,
            @RequestBody DevisRequest request) {
        return ResponseEntity.ok(devisService.updateDevis(id, request));
    }

    // GET /api/devis/{id} — get a single devis by its own ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN', 'TECHNICIAN', 'INFOLINE')")
    public ResponseEntity<DevisResponse> getDevisById(@PathVariable Long id) {
        return ResponseEntity.ok(devisService.getDevisById(id));
    }

    // GET /api/devis/ticket/{ticketId} — all devis for a ticket (history)
    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN', 'TECHNICIAN', 'INFOLINE')")
    public ResponseEntity<List<DevisResponse>> getDevisByTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(devisService.getDevisByTicket(ticketId));
    }

    // GET /api/devis/ticket/{ticketId}/latest — latest devis for a ticket
    @GetMapping("/ticket/{ticketId}/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN', 'TECHNICIAN', 'INFOLINE')")
    public ResponseEntity<DevisResponse> getLatestDevis(@PathVariable Long ticketId) {
        return ResponseEntity.ok(devisService.getLatestDevis(ticketId));
    }

    // PATCH /api/devis/lignes/{ligneId}/acceptee — infoline records client's accept/refuse per line
    @PatchMapping("/lignes/{ligneId}/acceptee")
    @PreAuthorize("hasAnyRole('INFOLINE', 'ADMIN')")
    public ResponseEntity<DevisLigneResponse> updateLigneAcceptance(
            @PathVariable Long ligneId,
            @RequestBody java.util.Map<String, Boolean> body) {
        return ResponseEntity.ok(devisService.updateLigneAcceptance(ligneId, body.get("acceptee")));
    }
}
