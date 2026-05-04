package com.repairshop.repair_ticket_system.service;

import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.entity.TicketStatus;
import com.repairshop.repair_ticket_system.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Daily job: for any ticket stuck in PRET_RETRAIT for ≥ 3 days, send a relance email
 * (max once every 3 days to avoid spam).
 *
 * Runs at 09:00 every day. Adjust via app.scheduler.pickup-reminder.cron.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PickupReminderScheduler {

    private final TicketRepository ticketRepository;
    private final EmailService     emailService;

    private static final int DAYS_BEFORE_FIRST_RELANCE = 3;
    private static final int DAYS_BETWEEN_RELANCES     = 3;

    @Scheduled(cron = "${app.scheduler.pickup-reminder.cron:0 0 9 * * *}")
    public void sendPickupReminders() {
        LocalDateTime now           = LocalDateTime.now();
        LocalDateTime before        = now.minusDays(DAYS_BEFORE_FIRST_RELANCE);
        LocalDateTime relanceBefore = now.minusDays(DAYS_BETWEEN_RELANCES);

        List<Ticket> stale = ticketRepository.findStaleAwaitingPickup(
                TicketStatus.PRET_RETRAIT, before, relanceBefore);

        if (stale.isEmpty()) {
            log.info("Pickup reminder scheduler: no stale tickets to relaunch.");
            return;
        }
        log.info("Pickup reminder scheduler: relancing {} ticket(s).", stale.size());
        for (Ticket t : stale) {
            emailService.sendPickupReminderEmail(t);
            t.setLastPickupRelanceAt(now);
            ticketRepository.save(t);
        }
    }
}
