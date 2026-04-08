package com.repairshop.repair_ticket_system.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "devis_lignes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevisLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "devis_id", nullable = false)
    private Devis devis;

    private String description;     // Name of the part or service
    private Integer quantite;       // Quantity
    private Double prixUnitaire;    // Unit price (HT)

    // Whether the client accepted this specific line
    private Boolean acceptee;       // null = pending, true = accepted, false = refused
}
