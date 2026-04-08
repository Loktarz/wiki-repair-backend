package com.repairshop.repair_ticket_system.dto;

import lombok.Data;

import java.util.List;

@Data
public class DevisRequest {

    private Long ticketId;

    private List<DevisLigneRequest> lignes; // Individual parts/services
    private Double mainDoeuvre;             // Labor cost HT
    private String notes;
}
