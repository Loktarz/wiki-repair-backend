package com.repairshop.repair_ticket_system.repository;

import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.entity.TicketStatus;
import com.repairshop.repair_ticket_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByAgentMagasin(User agentMagasin);
    List<Ticket> findByTechnician(User technician);

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByClientPhoneContaining(String phone);

    // Count tickets created in a given year for sequential numbering
    @Query("SELECT COUNT(t) FROM Ticket t WHERE YEAR(t.createdAt) = :year")
    long countByYear(int year);
}