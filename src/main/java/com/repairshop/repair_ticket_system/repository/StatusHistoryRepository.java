package com.repairshop.repair_ticket_system.repository;

import com.repairshop.repair_ticket_system.entity.StatusHistory;
import com.repairshop.repair_ticket_system.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByTicketOrderByChangedAtDesc(Ticket ticket);
}
