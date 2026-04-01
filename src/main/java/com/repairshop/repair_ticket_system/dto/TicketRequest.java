package com.repairshop.repair_ticket_system.dto;

import lombok.Data;

@Data
public class TicketRequest {

    // Product info
    private String productFamily;   // IT, HIFI, Téléphonie
    private String productType;     // Desktop, Laptop, Smartphone, etc.
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

    // Service type
    private String serviceType;     // Diagnostic, Réparation, Montage, etc.

    // Who is the client? (used when clerk creates ticket on behalf of a client)
    private Long clientId;
}
