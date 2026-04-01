package com.repairshop.repair_ticket_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Product Info ---
    private String productFamily;   // IT, HIFI, Téléphonie
    private String productType;     // Desktop, Laptop, Smartphone, etc.
    private String brand;
    private String designation;
    private String reference;
    private String serialNumber;

    // --- Problem ---
    private String problemDescription;
    private String accessories;     // List of accessories provided with the device
    private String machineState;    // Physical condition of the device

    // --- Warranty ---
    private Boolean warrantyPieces;
    private Boolean warrantyLabor;

    // --- Service ---
    private String serviceType;     // Diagnostic, Réparation, Montage, etc.

    // --- Status ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    // --- Actors ---
    @ManyToOne
    @JoinColumn(name = "client_id")
    private User client;

    @ManyToOne
    @JoinColumn(name = "clerk_id")
    private User clerk;

    @ManyToOne
    @JoinColumn(name = "technician_id")
    private User technician;

    // --- Timestamps ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = TicketStatus.EN_ATTENTE_DEPOT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
