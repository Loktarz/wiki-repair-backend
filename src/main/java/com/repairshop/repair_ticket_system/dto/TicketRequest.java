package com.repairshop.repair_ticket_system.dto;

import com.repairshop.repair_ticket_system.entity.ClientType;
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

    // Client info (external person, not a system user)
    private ClientType clientType;  // PARTICULIER or ENTREPRISE
    private String clientName;
    private String clientPhone;
    private String clientEmail;
    private String clientAddress;
    private String clientCompany;

    // Diagnostic notes (technician fills this after diagnosis)
    private String diagnosticNotes;
}
