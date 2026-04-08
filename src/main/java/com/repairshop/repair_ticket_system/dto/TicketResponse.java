package com.repairshop.repair_ticket_system.dto;

import com.repairshop.repair_ticket_system.entity.ClientType;
import com.repairshop.repair_ticket_system.entity.TicketStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TicketResponse {

    private Long id;
    private String ticketNumber;    // e.g. WR-2025-000042

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

    // Client info (external person, not a system user)
    private ClientType clientType;
    private String clientName;
    private String clientPhone;
    private String clientEmail;
    private String clientAddress;
    private String clientCompany;

    // Diagnostic
    private String diagnosticNotes;

    // Actors
    private Long agentMagasinId;
    private String agentMagasinName;

    private Long infolineId;
    private String infolineName;

    private Long technicianId;
    private String technicianName;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
