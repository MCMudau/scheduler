package com.mphoYanga.scheduler.services;

import com.mphoYanga.scheduler.models.Client;
import com.mphoYanga.scheduler.repos.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class ClientService {

    @Autowired private ClientRepository clientRepository;
    @Autowired private PasswordEncoder   passwordEncoder;
    @Autowired private EmailService      emailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Registration ─────────────────────────────────────────────────

    /**
     * Register a new client, generate a 5-digit PIN and email it.
     *
     * @throws IllegalArgumentException if email already exists or passwords don't match
     */
    public Client register(String name, String surname, String email,
                           String phoneNumber, String address,
                           String password, String confirmPassword) {

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (clientRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        Client client = new Client();
        client.setName(name);
        client.setSurname(surname);
        client.setEmail(email);
        client.setPhoneNumber(phoneNumber);
        client.setAddress(address);
        client.setPassword(passwordEncoder.encode(password));

        String pin = generatePin();
        client.setVerificationPin(pin);
        client.setPinExpiresAt(LocalDateTime.now().plusHours(24));

        clientRepository.save(client);

        emailService.sendClientVerificationPin(email, name, pin);

        return client;
    }

    // ── PIN Verification ─────────────────────────────────────────────

    /**
     * Verify the PIN submitted by the client.
     *
     * @return true if correct and not expired; false otherwise
     * @throws IllegalArgumentException if no account found for the email
     */
    public boolean verifyPin(String email, String submittedPin) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found for this email."));

        if (client.isVerified()) {
            // Already verified — treat as success so they can proceed
            return true;
        }

        if (client.getPinExpiresAt().isBefore(LocalDateTime.now())) {
            return false; // PIN expired
        }

        if (!client.getVerificationPin().equals(submittedPin)) {
            return false; // Wrong PIN
        }

        client.setVerified(true);
        client.setVerificationPin(null);   // clear used PIN
        client.setPinExpiresAt(null);
        clientRepository.save(client);
        return true;
    }

    // ── Resend PIN ────────────────────────────────────────────────────

    /**
     * Generate a fresh PIN and resend the verification email.
     *
     * @throws IllegalArgumentException if no account found or already verified
     */
    public void resendPin(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found for this email."));

        if (client.isVerified()) {
            throw new IllegalArgumentException("This account is already verified.");
        }

        String pin = generatePin();
        client.setVerificationPin(pin);
        client.setPinExpiresAt(LocalDateTime.now().plusHours(24));
        clientRepository.save(client);

        emailService.sendClientVerificationPin(email, client.getName(), pin);
    }

    // ── Login ─────────────────────────────────────────────────────────

    /**
     * Authenticate a client by email and password.
     *
     * @return the matched Client entity on success
     * @throws IllegalArgumentException if credentials are invalid or account not found
     */
    public Client loginClient(String email, String password) {

        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No account found for this email address."));

        if (!passwordEncoder.matches(password, client.getPassword())) {
            throw new IllegalArgumentException(
                    "Incorrect password. Please try again.");
        }

        return client;
    }

    // ── Helper ───────────────────────────────────────────────────────

    /** Generates a zero-padded 5-digit PIN, e.g. "03847". */
    private String generatePin() {
        return String.format("%05d", RANDOM.nextInt(100_000));
    }
}