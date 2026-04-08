package com.repairshop.repair_ticket_system.repository;

import com.repairshop.repair_ticket_system.entity.Devis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DevisRepository extends JpaRepository<Devis, Long> {

    // A ticket can have multiple devis revisions — return the latest
    List<Devis> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    Optional<Devis> findTopByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
