package com.repairshop.repair_ticket_system.controller;

import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.entity.TicketStatus;
import com.repairshop.repair_ticket_system.repository.TicketRepository;
import com.repairshop.repair_ticket_system.service.BonReceptionService;
import com.repairshop.repair_ticket_system.service.FileStorageService;
import com.repairshop.repair_ticket_system.service.TicketService;
import com.repairshop.repair_ticket_system.dto.TicketStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * Endpoints for the Bon de Réception PDF workflow:
 *   GET  /api/tickets/{id}/bon-reception           → download generated PDF
 *   POST /api/tickets/{id}/bon-reception/signed    → upload signed scan
 *   GET  /api/tickets/{id}/bon-reception/signed    → re-download the uploaded scan
 */
@RestController
@RequestMapping("/api/tickets/{id}/bon-reception")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class BonReceptionController {

    private final BonReceptionService bonReceptionService;
    private final FileStorageService  fileStorageService;
    private final TicketRepository    ticketRepository;
    private final TicketService       ticketService;

    /** Download the generated Bon de Réception PDF (assigns a unique bonNumber on first call). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN', 'INFOLINE')")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + id));

        byte[] pdf = bonReceptionService.generate(id);
        ticket = ticketRepository.findById(id).orElseThrow();
        String filename = "BonReception_" + ticket.getBonNumber() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", filename);
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    /** Upload the scanned signed copy. On success, transitions ticket to FICHE_REPARATION_IMPRIMEE. */
    @PostMapping("/signed")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN')")
    public ResponseEntity<?> uploadSigned(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal) {

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + id));

        String relPath = fileStorageService.storeSignedBon(file, ticket.getTicketNumber());
        ticket.setSignedBonPath(relPath);
        ticket.setSignedBonUploadedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        // Auto-transition to FICHE_REPARATION_IMPRIMEE if the current status allows it
        if (ticket.getStatus() == TicketStatus.DEPOSE_MAGASIN
                || ticket.getStatus() == TicketStatus.EN_ATTENTE_DEPOT) {
            TicketStatusUpdateRequest req = new TicketStatusUpdateRequest();
            req.setStatus(TicketStatus.FICHE_REPARATION_IMPRIMEE);
            ticketService.updateStatus(id, req, principal.getName());
        }

        return ResponseEntity.ok(java.util.Map.of(
                "message", "Bon signé téléversé avec succès",
                "signedBonPath", relPath,
                "newStatus", "FICHE_REPARATION_IMPRIMEE"
        ));
    }

    /** Re-download the uploaded signed scan. */
    @GetMapping("/signed")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN', 'TECHNICIAN', 'INFOLINE')")
    public ResponseEntity<byte[]> downloadSigned(@PathVariable Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + id));
        if (ticket.getSignedBonPath() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] pdf = fileStorageService.read(ticket.getSignedBonPath());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline",
                "Signed_BonReception_" + ticket.getBonNumber() + ".pdf");
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
