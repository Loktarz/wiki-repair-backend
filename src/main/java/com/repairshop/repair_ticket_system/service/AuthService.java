package com.repairshop.repair_ticket_system.service;

import com.repairshop.repair_ticket_system.dto.AuthResponse;
import com.repairshop.repair_ticket_system.dto.LoginRequest;
import com.repairshop.repair_ticket_system.dto.RegisterRequest;
import com.repairshop.repair_ticket_system.entity.User;
import com.repairshop.repair_ticket_system.repository.UserRepository;
import com.repairshop.repair_ticket_system.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ─── Register ──────────────────────────────────────────────────────────────

    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use: " + request.getEmail());
        }

        // Build and save the new user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // hash the password
                .phone(request.getPhone())
                .role(request.getRole())
                .build();

        userRepository.save(user);

        // Generate JWT and return
        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    // ─── Login ─────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        // This throws an exception automatically if email/password are wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // If we reach here, credentials are valid — load user and generate token
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
