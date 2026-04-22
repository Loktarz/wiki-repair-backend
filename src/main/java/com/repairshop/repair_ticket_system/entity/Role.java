package com.repairshop.repair_ticket_system.entity;

public enum Role {
    ADMIN,
    AGENT_MAGASIN, // Agent magasin
    TECHNICIAN,    // Agent technicien
    INFOLINE,      // Agent infoline
    CLIENT         // Client self-registered via frontend
}