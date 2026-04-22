package com.repairshop.repair_ticket_system.controller;

import com.repairshop.repair_ticket_system.dto.AuthResponse;
import com.repairshop.repair_ticket_system.dto.LoginRequest;
import com.repairshop.repair_ticket_system.dto.RegisterRequest;
import com.repairshop.repair_ticket_system.entity.Role;
import com.repairshop.repair_ticket_system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/login — public, no token required
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // POST /api/auth/register — public, client self-registration (always CLIENT role)
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        request.setRole(Role.CLIENT);
        return ResponseEntity.ok(authService.register(request));
    }
}
