package com.repairshop.repair_ticket_system.repository;

import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.entity.TicketStatus;
import com.repairshop.repair_ticket_system.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
    // archived flag: false = active only (also matches legacy NULL rows), true = archived only, null = both
    // NOTE: PostgreSQL 18+ refuses to infer the type of a null bind parameter inside LOWER(...).
    // The CAST(:search AS string) hints Hibernate to pass it as text, which avoids
    //   "function lower(bytea) does not exist".
    @Query("SELECT t FROM Ticket t WHERE " +
           "(CAST(:search AS string) IS NULL " +
           "  OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "  OR LOWER(t.clientName)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "  OR LOWER(t.brand)        LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:archived IS NULL " +
           "     OR (:archived = false AND (t.archived IS NULL OR t.archived = false)) " +
           "     OR (:archived = true  AND t.archived = true))")
    Page<Ticket> findAllFiltered(
            @Param("search")   String search,
            @Param("status")   TicketStatus status,
            @Param("archived") Boolean archived,
            Pageable pageable);

    // Tickets stuck in PRET_RETRAIT for >threshold days, no relance recently → for scheduler
    @Query("SELECT t FROM Ticket t WHERE t.status = :status " +
           "AND t.pretRetraitAt IS NOT NULL AND t.pretRetraitAt <= :before " +
           "AND (t.lastPickupRelanceAt IS NULL OR t.lastPickupRelanceAt <= :relanceBefore)")
    List<Ticket> findStaleAwaitingPickup(
            @Param("status") TicketStatus status,
            @Param("before") LocalDateTime before,
            @Param("relanceBefore") LocalDateTime relanceBefore);

    // Filter by archived flag
    Page<Ticket> findByArchived(Boolean archived, Pageable pageable);
}