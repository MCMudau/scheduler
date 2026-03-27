package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.services.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the forgot-password / PIN reset flow.
 *
 * All three steps share the same base path: /api/password-reset
 *
 * Step 1 — POST /api/password-reset/request
 *           Body: { "role": "admin"|"client", "email": "..." }
 *           → generates a PIN, persists it, emails it.
 *
 * Step 2 — POST /api/password-reset/verify-pin
 *           Body: { "role": "...", "email": "...", "pin": "12345" }
 *           → validates the PIN (correct & not expired).
 *
 * Step 3 — POST /api/password-reset/reset-password
 *           Body: { "role": "...", "email": "...", "newPassword": "..." }
 *           → hashes & saves the new password, clears the PIN.
 *
 * All endpoints return JSON: { "success": true|false, "message": "..." }
 */
@RestController
@RequestMapping("/api/password-reset")
public class PasswordResetController {

    private final PasswordResetService resetService;

    @Autowired
    public PasswordResetController(PasswordResetService resetService) {
        this.resetService = resetService;
    }

    // ── Step 1: Request a reset PIN ───────────────────────────────────────────

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestReset(@RequestBody Map<String, String> body) {
        String role  = body.get("role");
        String email = body.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Email address is required."));
        }
        if (role == null || role.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Role is required."));
        }

        Map<String, Object> result = resetService.requestReset(role.trim(), email.trim().toLowerCase());
        return ResponseEntity.ok(result);
    }

    // ── Step 2: Verify the PIN ────────────────────────────────────────────────

    @PostMapping("/verify-pin")
    public ResponseEntity<Map<String, Object>> verifyPin(@RequestBody Map<String, String> body) {
        String role  = body.get("role");
        String email = body.get("email");
        String pin   = body.get("pin");

        if (role == null || email == null || pin == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Role, email, and PIN are all required."));
        }

        Map<String, Object> result = resetService.verifyPin(
                role.trim(), email.trim().toLowerCase(), pin.trim()
        );
        return ResponseEntity.ok(result);
    }

    // ── Step 3: Set the new password ──────────────────────────────────────────

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> body) {
        String role        = body.get("role");
        String email       = body.get("email");
        String newPassword = body.get("newPassword");

        if (role == null || email == null || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Role, email, and new password are required."));
        }

        Map<String, Object> result = resetService.resetPassword(
                role.trim(), email.trim().toLowerCase(), newPassword
        );
        return ResponseEntity.ok(result);
    }
}
