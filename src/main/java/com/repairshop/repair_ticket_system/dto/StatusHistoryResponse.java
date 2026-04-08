package com.repairshop.repair_ticket_system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusHistoryResponse {
    private Long id;
    private String oldStatus;
    private String oldStatusLabel;
    private String newStatus;
    private String newStatusLabel;
    private String changedByName;
    private String changedByRole;
    private String changedAt;
}
