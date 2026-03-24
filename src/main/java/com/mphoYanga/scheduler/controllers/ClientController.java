package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.Client;
import com.mphoYanga.scheduler.repos.ClientRepository;
import com.mphoYanga.scheduler.services.ClientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/client")
public class ClientController {

    @Autowired private ClientService clientService;

    // ── POST /api/client/register ─────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body,HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name            = require(body, "name");
            String surname         = require(body, "surname");
            String email           = require(body, "email");
            String phoneNumber     = require(body, "phoneNumber");
            String address         = body.getOrDefault("address", "");
            String password        = require(body, "password");
            String confirmPassword = require(body, "confirmPassword");

            Client client=clientService.register(name, surname, email, phoneNumber, address, password, confirmPassword);

            session.setAttribute("userId",         client.getId());
            session.setAttribute("userName",       client.getName());
            session.setAttribute("userEmail",      client.getEmail());
            session.setAttribute("userRole",       "client");
            session.setAttribute("clientVerified", client.getVerified());

            response.put("success", true);
            response.put("message", "Account created successfully. Check your email for the verification PIN.");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "An unexpected error occurred. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ── POST /api/client/login ────────────────────────────────────────────────
    //
    // Accepts:  { "email": "...", "password": "..." }
    // Success:  saves session, returns redirect target
    // Failure:  401 with error message
    //
    // Can be called directly (role-specific login) OR via LoginController
    // which delegates here after reading the "role" field from the request body.

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        try {
            String email    = require(body, "email");
            String password = require(body, "password");

            Client client = clientService.loginClient(email, password);

            // Persist client identity in the server-side session
            session.setAttribute("userId",         client.getId());
            session.setAttribute("userName",       client.getName());
            session.setAttribute("userEmail",      client.getEmail());
            session.setAttribute("userRole",       "client");
            session.setAttribute("clientVerified", client.getVerified());

            response.put("success",  true);
            response.put("role",     "client");
            response.put("name",     client.getName());
            response.put("verified", client.getVerified());

            // Unverified clients must enter their PIN before accessing the dashboard
            response.put("redirect", Boolean.TRUE.equals(client.getVerified())
                    ? "/client-dashboard.html"
                    : "/verify-client.html");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(401).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Something went wrong. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ── POST /api/client/verify ───────────────────────────────────────────────

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> body,
                                                      HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = require(body, "email");
            String pin   = require(body, "pin");

            boolean ok = clientService.verifyPin(email, pin);

            if (ok) {
                // Mark the active session as verified so dashboards can gate on this
                session.setAttribute("clientVerified", true);

                response.put("success",  true);
                response.put("message",  "Account verified successfully.");
                response.put("redirect", "/client-dashboard.html");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Invalid or expired PIN. Please try again or request a new one.");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "An unexpected error occurred.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ── POST /api/client/resend-pin ───────────────────────────────────────────

    @PostMapping("/resend-pin")
    public ResponseEntity<Map<String, Object>> resendPin(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = require(body, "email");
            clientService.resendPin(email);

            response.put("success", true);
            response.put("message", "A new verification PIN has been sent to your email.");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to resend PIN. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ── GET /api/client/session ───────────────────────────────────────────────
    // Lightweight check called by the client dashboard on page load.

    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> sessionStatus(HttpSession session) {
        Map<String, Object> res = new HashMap<>();

        Long userId = (Long) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        if (userId == null || !"client".equals(role)) {
            res.put("loggedIn", false);
            return ResponseEntity.ok(res);
        }

        res.put("loggedIn",  true);
        res.put("id",        userId);
        res.put("name",      session.getAttribute("userName"));
        res.put("email",     session.getAttribute("userEmail"));
        res.put("role",      "client");
        res.put("verified",  session.getAttribute("clientVerified"));
        return ResponseEntity.ok(res);
    }

    // ── POST /api/client/logout ───────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of(
                "success",  true,
                "redirect", "/login.html"
        ));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String require(Map<String, String> map, String key) {
        String val = map.get(key);
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return val.trim();
    }
    @Autowired
    ClientRepository clientRepository;
    @GetMapping("/clients")
    public ResponseEntity<List<Client>> getAllClients() {
        try {
            List<Client> clients = clientRepository.findAll();
            return ResponseEntity.ok(clients);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Add this method to your ClientRestController.java

    /**
     * Get the current logged-in client
     * GET /api/client/current
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentClient(HttpSession session) {
        try {
            // Get client ID from session
            Long clientId = (Long) session.getAttribute("clientId");

            if (clientId == null) {
                // Try alternate session key
                clientId = (Long) session.getAttribute("userId");
            }

            if (clientId == null) {
                return ResponseEntity.status(401).body(new ApiResponse(false, "No client session found", null));
            }

            // Fetch the client from database
            Optional<Client> client = clientRepository.findById(clientId);

            if (client.isPresent()) {
                return ResponseEntity.ok(new ApiResponse(true, "Current client retrieved", client.get()));
            } else {
                return ResponseEntity.status(404).body(new ApiResponse(false, "Client not found", null));
            }
        } catch (Exception e) {
            System.out.println("[ClientController] Error getting current client: " + e.getMessage());
            return ResponseEntity.status(500).body(new ApiResponse(false, "Error: " + e.getMessage(), null));
        }
    }

    // Also add the ApiResponse class if not already present
    @lombok.AllArgsConstructor
    @lombok.Getter
    public static class ApiResponse {
        private Boolean success;
        private String message;
        private Object data;
    }
}