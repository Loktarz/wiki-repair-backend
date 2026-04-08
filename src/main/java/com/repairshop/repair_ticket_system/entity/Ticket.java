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

    // --- Ticket Number ---
    @Column(unique = true)
    private String ticketNumber;    // e.g. WR-2025-000042

    // --- Client Info (external, not a system user) ---
    @Enumerated(EnumType.STRING)
    private ClientType clientType;  // PARTICULIER or ENTREPRISE
    private String clientName;
    private String clientPhone;
    private String clientEmail;
    private String clientAddress;
    private String clientCompany;   // Filled if the client is a business (ENTREPRISE)

    // --- Diagnostic ---
    private String diagnosticNotes; // Technician's findings after diagnosis

    // --- Actors ---
    @ManyToOne
    @JoinColumn(name = "agent_magasin_id")
    private User agentMagasin;

    @ManyToOne
    @JoinColumn(name = "infoline_id")
    private User infoline;

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
