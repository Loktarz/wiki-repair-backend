package com.repairshop.repair_ticket_system.dto;

import lombok.Data;

@Data
public class DevisLigneRequest {
    private String description;
    private Integer quantite;
    private Double prixUnitaire;
}
