package com.mphoYanga.scheduler.services;

import com.mphoYanga.scheduler.models.Admin;
import com.mphoYanga.scheduler.repos.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // All verification PINs are sent to this address during development
    private static final String PIN_RECIPIENT = "mudaumuthusi@gmail.com";

    // ── REGISTRATION ──────────────────────────────────────────

    /**
     * Register a new admin account.
     *
     * @param name            first name
     * @param surname         last name
     * @param email           email address (must be unique)
     * @param phoneNumber     contact number
     * @param password        plain-text password
     * @param confirmPassword must match password
     * @return the saved Admin entity
     * @throws IllegalArgumentException if email is taken or passwords do not match
     */
    public Admin registerAdmin(String name,
                               String surname,
                               String email,
                               String phoneNumber,
                               String password,
                               String confirmPassword) {

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (adminRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        String pin = generatePin();

        Admin admin = new Admin();
        admin.setName(name);
        admin.setSurname(surname);
        admin.setEmail(email);
        admin.setPhoneNumber(phoneNumber);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setVerificationPin(pin);
        admin.setVerified(false);

        Admin saved = adminRepository.save(admin);

        emailService.sendVerificationPin(PIN_RECIPIENT, name, pin);

        return saved;
    }

    // ── LOGIN ─────────────────────────────────────────────────

    /**
     * Authenticate an admin by email and password.
     *
     * @param email    the entered email
     * @param password the entered plain-text password
     * @return the authenticated Admin entity
     * @throws IllegalArgumentException if credentials are invalid
     */
    public Admin loginAdmin(String email, String password) {

        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return admin;
    }

    // ── VERIFICATION ──────────────────────────────────────────

    /**
     * Verify an admin account using their 5-digit PIN.
     *
     * @param adminId    the admin's ID (from session)
     * @param enteredPin the PIN entered by the admin
     * @return the verified Admin entity
     * @throws IllegalArgumentException if PIN is wrong or admin not found
     */
    public Admin verifyAdmin(Long adminId, String enteredPin) {

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found"));

        if (admin.getVerified()) {
            return admin; // Already verified — just return
        }

        if (!admin.getVerificationPin().equals(enteredPin.trim())) {
            throw new IllegalArgumentException("Incorrect verification PIN. Please try again.");
        }

        admin.setVerified(true);
        admin.setVerificationPin(null); // Clear PIN after use for security
        return adminRepository.save(admin);
    }

    // ── RESEND PIN ────────────────────────────────────────────

    /**
     * Generate a fresh PIN and resend it to the admin.
     *
     * @param adminId the admin's ID (from session)
     * @throws IllegalArgumentException if admin not found
     * @throws IllegalStateException    if account is already verified
     */
    public void resendPin(Long adminId) {

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found"));

        if (admin.getVerified()) {
            throw new IllegalStateException("This account is already verified");
        }

        String newPin = generatePin();
        admin.setVerificationPin(newPin);
        adminRepository.save(admin);

        emailService.sendVerificationPin(PIN_RECIPIENT, admin.getName(), newPin);
    }

    // ── HELPERS ───────────────────────────────────────────────

    /**
     * Generate a cryptographically secure 5-digit PIN (10000–99999).
     */
    private String generatePin() {
        SecureRandom random = new SecureRandom();
        int pin = 10000 + random.nextInt(90000);
        return String.valueOf(pin);
    }

    /**
     * Find an admin by their ID.
     */
    public Optional<Admin> findById(Long id) {
        return adminRepository.findById(id);
    }

    /**
     * Check if an email address is already registered.
     */
    public boolean isEmailTaken(String email) {
        return adminRepository.existsByEmail(email);
    }
}