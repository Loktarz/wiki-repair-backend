package com.repairshop.repair_ticket_system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DevisLigneResponse {
    private Long id;
    private String description;
    private Integer quantite;
    private Double prixUnitaire;
    private Double totalHT;         // quantite * prixUnitaire
    private Boolean acceptee;       // null=pending, true=accepted, false=refused
}
