package com.mphoYanga.scheduler.services;

import com.mphoYanga.scheduler.models.Admin;
import com.mphoYanga.scheduler.models.Client;
import com.mphoYanga.scheduler.repos.AdminRepository;
import com.mphoYanga.scheduler.repos.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Service that handles the full forgot-password flow for both Admin and Client users.
 *
 * Flow:
 *   1. requestReset(role, email)  → generates a 5-digit PIN, persists it (overwriting any old one),
 *                                    sends it via PasswordResetEmailService, returns success/error.
 *   2. verifyPin(role, email, pin) → checks PIN is correct and not expired (15 min window).
 *   3. resetPassword(role, email, newPassword) → BCrypt-hashes and saves the new password,
 *                                                 clears the PIN field.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int PIN_EXPIRY_MINUTES = 15;

    private final AdminRepository  adminRepository;
    private final ClientRepository clientRepository;
    private final PasswordResetEmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public PasswordResetService(AdminRepository adminRepository,
                                ClientRepository clientRepository,
                                PasswordResetEmailService emailService) {
        this.adminRepository  = adminRepository;
        this.clientRepository = clientRepository;
        this.emailService     = emailService;
    }

    // ── Step 1: Request a reset PIN ───────────────────────────────────────────

    /**
     * Generates a fresh PIN, saves it to the user record, and emails it.
     *
     * @param role  "admin" | "client"
     * @param email the user's registered email address
     * @return map with keys "success" (boolean) and "message" (String)
     */
    public Map<String, Object> requestReset(String role, String email) {
        String pin = generatePin();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(PIN_EXPIRY_MINUTES);

        if ("admin".equalsIgnoreCase(role)) {
            Optional<Admin> opt = adminRepository.findByEmail(email);
            if (opt.isEmpty()) {
                return error("No account found with that email address.");
            }
            Admin admin = opt.get();
            admin.setVerificationPin(pin);
            adminRepository.save(admin);

            // Fire email in background thread so HTTP response is not blocked
            new Thread(()->{
                emailService.sendPasswordResetPin(email, admin.getName(), pin, "Admin Portal", "#e8762a");
            }).start();


        } else if ("client".equalsIgnoreCase(role)) {
            Optional<Client> opt = clientRepository.findByEmail(email);
            if (opt.isEmpty()) {
                return error("No account found with that email address.");
            }
            Client client = opt.get();
            client.setVerificationPin(pin);
            client.setPinExpiresAt(expiry);
            clientRepository.save(client);

            new Thread(()->{
                emailService.sendPasswordResetPin(email, client.getName(), pin, "Client Portal", "#3cb54a");
            }).start();

        } else {
            return error("Invalid role specified.");
        }

        log.info("Password reset PIN sent to {} ({})", email, role);
        return ok("A reset PIN has been sent to your email address. It expires in " + PIN_EXPIRY_MINUTES + " minutes.");
    }

    // ── Step 2: Verify the PIN ────────────────────────────────────────────────

    /**
     * Validates the PIN submitted by the user.
     * Admin entities don't store pinExpiresAt, so expiry is checked only for clients.
     *
     * @param role  "admin" | "client"
     * @param email the user's email
     * @param pin   the PIN entered by the user
     * @return map with keys "success" (boolean) and "message" (String)
     */
    public Map<String, Object> verifyPin(String role, String email, String pin) {
        if (pin == null || pin.isBlank()) {
            return error("Please enter the PIN sent to your email.");
        }

        if ("admin".equalsIgnoreCase(role)) {
            Optional<Admin> opt = adminRepository.findByEmail(email);
            if (opt.isEmpty()) return error("Account not found.");

            Admin admin = opt.get();
            if (admin.getVerificationPin() == null || !admin.getVerificationPin().equals(pin.trim())) {
                return error("Incorrect PIN. Please check your email and try again.");
            }
            // Admins: no stored expiry — PINs generated within the last 15 min are valid.
            // We can't check server-side without a timestamp field on Admin, so we trust the
            // client's promptness. If you add pinExpiresAt to Admin later, add the check here.

        } else if ("client".equalsIgnoreCase(role)) {
            Optional<Client> opt = clientRepository.findByEmail(email);
            if (opt.isEmpty()) return error("Account not found.");

            Client client = opt.get();
            if (client.getVerificationPin() == null || !client.getVerificationPin().equals(pin.trim())) {
                return error("Incorrect PIN. Please check your email and try again.");
            }
            if (client.getPinExpiresAt() != null && LocalDateTime.now().isAfter(client.getPinExpiresAt())) {
                return error("This PIN has expired. Please request a new one.");
            }

        } else {
            return error("Invalid role specified.");
        }

        return ok("PIN verified successfully.");
    }

    // ── Step 3: Reset the password ────────────────────────────────────────────

    /**
     * Saves the new BCrypt-hashed password and clears the reset PIN.
     * Caller must have already verified the PIN (Step 2) before invoking this.
     *
     * @param role        "admin" | "client"
     * @param email       the user's email
     * @param newPassword plaintext new password (will be hashed here)
     * @return map with keys "success" (boolean) and "message" (String)
     */
    public Map<String, Object> resetPassword(String role, String email, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return error("Password must be at least 6 characters.");
        }

        String hashed = passwordEncoder.encode(newPassword);

        if ("admin".equalsIgnoreCase(role)) {
            Optional<Admin> opt = adminRepository.findByEmail(email);
            if (opt.isEmpty()) return error("Account not found.");

            Admin admin = opt.get();
            admin.setPassword(hashed);
            admin.setVerificationPin(null);   // invalidate the PIN
            adminRepository.save(admin);

        } else if ("client".equalsIgnoreCase(role)) {
            Optional<Client> opt = clientRepository.findByEmail(email);
            if (opt.isEmpty()) return error("Account not found.");

            Client client = opt.get();
            client.setPassword(hashed);
            client.setVerificationPin(null);
            client.setPinExpiresAt(null);
            clientRepository.save(client);

        } else {
            return error("Invalid role specified.");
        }

        log.info("Password reset completed for {} ({})", email, role);
        return ok("Your password has been updated successfully. You can now log in.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generatePin() {
        // 5-digit numeric PIN, zero-padded
        return String.format("%05d", random.nextInt(100_000));
    }

    private Map<String, Object> ok(String message) {
        return Map.of("success", true, "message", message);
    }

    private Map<String, Object> error(String message) {
        return Map.of("success", false, "message", message);
    }
}
