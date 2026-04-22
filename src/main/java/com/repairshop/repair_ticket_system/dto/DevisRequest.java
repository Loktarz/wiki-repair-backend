package com.repairshop.repair_ticket_system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DevisRequest {

    @NotNull(message = "L'identifiant du ticket est obligatoire")
    private Long ticketId;

    @Valid
    private List<DevisLigneRequest> lignes;

    @NotNull(message = "Le coût de main d'oeuvre est obligatoire")
    @DecimalMin(value = "0.0", message = "Le coût de main d'oeuvre ne peut pas être négatif")
    private Double mainDoeuvre;

    private String notes;
}
