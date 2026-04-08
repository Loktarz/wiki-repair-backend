package com.repairshop.repair_ticket_system.controller;

import com.repairshop.repair_ticket_system.dto.PublicDemandeRequest;
import com.repairshop.repair_ticket_system.dto.PublicTrackResponse;
import com.repairshop.repair_ticket_system.dto.TicketResponse;
import java.util.List;
import com.repairshop.repair_ticket_system.service.PublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class PublicController {

    private final PublicService publicService;

    // GET /api/public/track/{ticketNumber} — client tracks their repair
    @GetMapping("/track/{ticketNumber}")
    public ResponseEntity<PublicTrackResponse> trackTicket(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(publicService.trackTicket(ticketNumber));
    }

    // GET /api/public/track-by-phone?phone=216XXXXXXX — find all tickets by phone
    @GetMapping("/track-by-phone")
    public ResponseEntity<List<PublicTrackResponse>> trackByPhone(@RequestParam String phone) {
        return ResponseEntity.ok(publicService.trackByPhone(phone));
    }

    // POST /api/public/demande — client submits a repair request online
    @PostMapping("/demande")
    public ResponseEntity<TicketResponse> submitDemande(@RequestBody PublicDemandeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(publicService.submitDemande(request));
    }
}
