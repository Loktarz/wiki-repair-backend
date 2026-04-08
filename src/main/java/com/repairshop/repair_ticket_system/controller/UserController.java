package com.repairshop.repair_ticket_system.controller;

import com.repairshop.repair_ticket_system.dto.RegisterRequest;
import com.repairshop.repair_ticket_system.dto.UserResponse;
import com.repairshop.repair_ticket_system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class UserController {

    private final UserService userService;

    // GET /api/users — admin sees all users
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // GET /api/users/by-role?role=TECHNICIAN — all staff can fetch users by role (for dropdowns)
    @GetMapping("/by-role")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_MAGASIN', 'TECHNICIAN', 'INFOLINE')")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@RequestParam String role) {
        com.repairshop.repair_ticket_system.entity.Role r =
                com.repairshop.repair_ticket_system.entity.Role.valueOf(role);
        return ResponseEntity.ok(userService.getUsersByRole(r));
    }

    // GET /api/users/me — any logged-in user sees their own profile
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getName()));
    }

    // GET /api/users/{id} — admin only
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // POST /api/users — admin creates a new user (clerk, technician, infoline)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    // PATCH /api/users/me/password — logged-in user changes own password
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            Principal principal,
            @RequestBody java.util.Map<String, String> body) {
        userService.changePassword(principal.getName(), body.get("currentPassword"), body.get("newPassword"));
        return ResponseEntity.ok().build();
    }

    // PATCH /api/users/{id}/role — admin updates a user's role
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        com.repairshop.repair_ticket_system.entity.Role role =
                com.repairshop.repair_ticket_system.entity.Role.valueOf(body.get("role"));
        return ResponseEntity.ok(userService.updateUserRole(id, role));
    }

    // DELETE /api/users/{id} — admin only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
