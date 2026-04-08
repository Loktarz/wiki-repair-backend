package com.repairshop.repair_ticket_system.dto;

import lombok.Data;

@Data
public class PublicDemandeRequest {
    // Client info
    private String clientType;   // PARTICULIER or ENTREPRISE
    private String clientName;
    private String clientPhone;
    private String clientEmail;
    private String clientAddress;
    private String clientCompany;

    // Device info
    private String productFamily;
    private String productType;
    private String brand;
    private String designation;
    private String serialNumber;
    private String machineState;
    private String accessories;

    // Problem
    private String problemDescription;
    private String serviceType;
}
