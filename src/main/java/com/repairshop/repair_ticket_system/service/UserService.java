package com.repairshop.repair_ticket_system.service;

import com.repairshop.repair_ticket_system.dto.RegisterRequest;
import com.repairshop.repair_ticket_system.dto.UserResponse;
import com.repairshop.repair_ticket_system.dto.UserUpdateRequest;
import com.repairshop.repair_ticket_system.entity.Ticket;
import com.repairshop.repair_ticket_system.entity.Role;
import com.repairshop.repair_ticket_system.entity.User;
import com.repairshop.repair_ticket_system.repository.TicketRepository;
import com.repairshop.repair_ticket_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Get all users (admin only) — excludes CLIENT role ────────────────────
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() != Role.CLIENT)
                .map(this::toResponse)
                .toList();
    }

    // ─── Get user by ID ────────────────────────────────────────────────────────
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return toResponse(user);
    }

    // ─── Get current logged-in user profile ───────────────────────────────────
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return toResponse(user);
    }

    // ─── Create user (admin creates clerks, technicians, etc.) ────────────────
    public UserResponse createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .build();

        return toResponse(userRepository.save(user));
    }

    // ─── Get users by role (e.g. all technicians) ─────────────────────────────
    public List<UserResponse> getUsersByRole(com.repairshop.repair_ticket_system.entity.Role role) {
        return userRepository.findByRole(role)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Update own profile (name + phone) ────────────────────────────────────
    public UserResponse updateCurrentUser(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        return toResponse(userRepository.save(user));
    }

    // ─── Change own password ───────────────────────────────────────────────────
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ─── Update user role ──────────────────────────────────────────────────────
    public UserResponse updateUserRole(Long id, com.repairshop.repair_ticket_system.entity.Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    // ─── Delete user ───────────────────────────────────────────────────────────
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Nullify references in tickets before deleting
        for (Ticket t : ticketRepository.findByAgentMagasin(user)) {
            t.setAgentMagasin(null); ticketRepository.save(t);
        }
        for (Ticket t : ticketRepository.findByTechnician(user)) {
            t.setTechnician(null); ticketRepository.save(t);
        }

        userRepository.deleteById(id);
    }

    // ─── Map Entity → DTO ──────────────────────────────────────────────────────
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
