package com.repairshop.repair_ticket_system.controller;

import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.repository.TicketRepository;
import com.repairshop.repair_ticket_system.service.FactureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint to download the final invoice (Facture WIKI) for a ticket.
 *
 * Pricing rules (handled by {@link FactureService}):
 *   LIVRE_CLIENT          → full devis amount (pieces + main d'œuvre + 19% TVA)
 *   DEVIS_REFUSE          → diagnostic fee only (20 DT)
 *   REPARATION_IMPOSSIBLE → diagnostic fee only (20 DT)
 *   any other status      → 400 / "Ticket non facturable au statut actuel"
 */
@RestController
@RequestMapping("/api/tickets/{id}/facture")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class FactureController {

    private final FactureService   factureService;
    private final TicketRepository ticketRepository;

    /** GET /api/tickets/{id}/facture — download the invoice PDF inline. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN', 'INFOLINE')")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + id));

        byte[] pdf = factureService.generate(id);
        String filename = "Facture_" + ticket.getTicketNumber() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", filename);
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
