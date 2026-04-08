package com.repairshop.repair_ticket_system.controller;

import com.repairshop.repair_ticket_system.dto.AuthResponse;
import com.repairshop.repair_ticket_system.dto.LoginRequest;
import com.repairshop.repair_ticket_system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/login — public, no token required
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
