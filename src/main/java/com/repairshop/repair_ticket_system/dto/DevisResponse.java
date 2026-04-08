package com.repairshop.repair_ticket_system.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DevisResponse {

    private Long id;
    private Long ticketId;

    // Line items
    private List<DevisLigneResponse> lignes;

    // Amounts
    private Double mainDoeuvre;
    private Double totalPiecesHT;
    private Double tva;
    private Double montantTotal;

    private String notes;

    // Who created it
    private Long createdById;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
