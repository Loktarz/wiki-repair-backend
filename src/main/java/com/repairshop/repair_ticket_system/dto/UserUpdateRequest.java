package com.repairshop.repair_ticket_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String fullName;

    @Pattern(regexp = "^$|^(\\+216\\s?)?[0-9]{8}$", message = "Numéro de téléphone invalide (8 chiffres requis)")
    private String phone;
}
