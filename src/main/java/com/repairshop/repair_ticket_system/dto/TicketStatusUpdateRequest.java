package com.repairshop.repair_ticket_system.dto;

import com.repairshop.repair_ticket_system.entity.TicketStatus;
import lombok.Data;

@Data
public class TicketStatusUpdateRequest {
    private TicketStatus status;
}
