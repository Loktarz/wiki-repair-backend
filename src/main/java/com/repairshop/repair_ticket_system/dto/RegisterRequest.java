package com.repairshop.repair_ticket_system.dto;

import com.repairshop.repair_ticket_system.entity.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private Role role;
}
