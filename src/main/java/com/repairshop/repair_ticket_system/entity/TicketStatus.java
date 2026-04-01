package com.repairshop.repair_ticket_system.entity;

public enum TicketStatus {

    // Phase 1 - Pré-réception
    EN_ATTENTE_DEPOT,
    DEPOSE_MAGASIN,
    FICHE_REPARATION_IMPRIMEE,

    // Phase 2 - Diagnostic
    DIAGNOSTIC_EN_ATTENTE,
    EN_DIAGNOSTIC,
    DIAGNOSTIC_TERMINE,

    // Phase 3 - Devis
    DEVIS_EN_ATTENTE,
    DEVIS_ENVOYE_CLIENT,
    DEVIS_ACCEPTE,
    DEVIS_REFUSE,

    // Phase 4 - Réparation
    TENTATIVE_REPARATION,
    ATTENTE_PIECE,
    PIECE_RECUE,
    EN_REPARATION,
    REPARATION_TERMINEE,

    // Phase 5 - Livraison
    PRET_RETRAIT,
    LIVRE_CLIENT,

    // Phase 6 - Cas spécial
    REPARATION_IMPOSSIBLE
}