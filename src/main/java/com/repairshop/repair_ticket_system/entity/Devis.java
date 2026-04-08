package com.repairshop.repair_ticket_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Devis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Link to ticket ---
    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    // --- Line items (one row per part/service) ---
    @OneToMany(mappedBy = "devis", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DevisLigne> lignes = new ArrayList<>();

    // --- Labor cost (separate from parts) ---
    private Double mainDoeuvre;     // Labor cost HT

    // --- Totals (auto-calculated) ---
    private Double totalPiecesHT;   // Sum of all accepted/all ligne totals
    private Double tva;             // 19% on (totalPiecesHT + mainDoeuvre)
    private Double montantTotal;    // Grand total TTC

    // --- Notes ---
    @Column(columnDefinition = "TEXT")
    private String notes;

    // --- Who created it ---
    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;         // Infoline agent

    // --- Timestamps ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Called explicitly by service after lignes are saved
    public void recalculate() {
        totalPiecesHT = lignes.stream()
                .mapToDouble(l -> {
                    int qty   = l.getQuantite()      != null ? l.getQuantite()      : 0;
                    double pu = l.getPrixUnitaire()  != null ? l.getPrixUnitaire()  : 0.0;
                    return qty * pu;
                })
                .sum();
        double labor = mainDoeuvre != null ? mainDoeuvre : 0.0;
        tva          = (totalPiecesHT + labor) * 0.19;
        montantTotal = totalPiecesHT + labor + tva;
    }
}
