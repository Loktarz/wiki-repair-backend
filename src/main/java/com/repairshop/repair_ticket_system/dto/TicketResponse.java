package com.repairshop.repair_ticket_system.dto;

import com.repairshop.repair_ticket_system.entity.TicketStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TicketResponse {

    private Long id;

    // Product info
    private String productFamily;
    private String productType;
    private String brand;
    private String designation;
    private String reference;
    private String serialNumber;

    // Problem
    private String problemDescription;
    private String accessories;
    private String machineState;

    // Warranty
    private Boolean warrantyPieces;
    private Boolean warrantyLabor;

    // Service
    private String serviceType;

    // Status
    private TicketStatus status;

    // Actors — we expose names/emails only, not full User objects
    private Long clientId;
    private String clientName;
    private String clientEmail;

    private Long clerkId;
    private String clerkName;

    private Long technicianId;
    private String technicianName;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
