package com.repairshop.repair_ticket_system.repository;

import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.entity.TicketStatus;
import com.repairshop.repair_ticket_system.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByAgentMagasin(User agentMagasin);
    List<Ticket> findByTechnician(User technician);

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByClientPhoneContaining(String phone);

    List<Ticket> findByClientEmailOrderByCreatedAtDesc(String email);

    // Count tickets created in a given year for sequential numbering
    @Query("SELECT COUNT(t) FROM Ticket t WHERE YEAR(t.createdAt) = :year")
    long countByYear(int year);

    // Paginated query with optional search (ticketNumber, clientName, brand) and optional status filter
    @Query("SELECT t FROM Ticket t WHERE " +
           "(:search IS NULL OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.clientName)    LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.brand)         LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR t.status = :status)")
    Page<Ticket> findAllFiltered(
            @Param("search") String search,
            @Param("status") TicketStatus status,
            Pageable pageable);
}