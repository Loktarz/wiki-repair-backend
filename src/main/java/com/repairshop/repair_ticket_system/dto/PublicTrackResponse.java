package com.repairshop.repair_ticket_system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicTrackResponse {
    private String ticketNumber;
    private String clientName;       // shown partially masked on frontend
    private String productType;
    private String brand;
    private String serviceType;
    private String status;           // raw enum name
    private String phase;            // DEPOT | DIAGNOSTIC | DEVIS | REPARATION | PRET | LIVRE | IMPOSSIBLE
    private String phaseLabel;       // Human-readable French label
    private String statusLabel;      // Human-readable French status
    private String createdAt;
    private String updatedAt;
}
