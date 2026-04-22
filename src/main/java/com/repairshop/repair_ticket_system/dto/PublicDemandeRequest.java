package com.repairshop.repair_ticket_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PublicDemandeRequest {

    // Client info
    @NotBlank(message = "Le type de client est obligatoire")
    private String clientType;

    @NotBlank(message = "Le nom du client est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String clientName;

    @NotBlank(message = "Le téléphone du client est obligatoire")
    @Pattern(regexp = "^(\\+216\\s?)?[0-9]{8}$", message = "Numéro de téléphone invalide (8 chiffres requis)")
    private String clientPhone;

    @Email(message = "Format d'email invalide")
    private String clientEmail;

    @Size(max = 300, message = "L'adresse ne peut pas dépasser 300 caractères")
    private String clientAddress;

    @Size(max = 150, message = "Le nom de société ne peut pas dépasser 150 caractères")
    private String clientCompany;

    // Device info
    @NotBlank(message = "La famille de produit est obligatoire")
    private String productFamily;

    @NotBlank(message = "Le type de produit est obligatoire")
    private String productType;

    private String brand;

    @Size(max = 200, message = "La désignation ne peut pas dépasser 200 caractères")
    private String designation;

    private String serialNumber;
    private String machineState;

    @Size(max = 500, message = "Les accessoires ne peuvent pas dépasser 500 caractères")
    private String accessories;

    // Problem
    @NotBlank(message = "La description du problème est obligatoire")
    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    private String problemDescription;

    @NotBlank(message = "Le type de service est obligatoire")
    private String serviceType;
}
