package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.Admin;
import com.mphoYanga.scheduler.models.Client;
import com.mphoYanga.scheduler.services.AdminService;
import com.mphoYanga.scheduler.services.ClientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * LoginController
 *
 * Central login entry-point consumed by login.html.
 *
 * POST /api/login
 * Body: { "role": "admin" | "client", "email": "...", "password": "..." }
 *
 * The controller reads the "role" field and delegates to the appropriate
 * service, keeping AdminService and ClientService fully decoupled.
 */
@RestController
@RequestMapping("/api")
public class LoginController {

    private final AdminService  adminService;
    private final ClientService clientService;

    @Autowired
    public LoginController(AdminService adminService, ClientService clientService) {
        this.adminService  = adminService;
        this.clientService = clientService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/login
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        Map<String, Object> res = new HashMap<>();

        // ── 1. Extract & validate common fields ──────────────────────────────
        String role     = body.get("role");
        String email    = body.get("email");
        String password = body.get("password");

        if (role == null || role.isBlank()) {
            res.put("success", false);
            res.put("message", "Please select a role before signing in.");
            return ResponseEntity.badRequest().body(res);
        }

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            res.put("success", false);
            res.put("message", "Email and password are required.");
            return ResponseEntity.badRequest().body(res);
        }

        // ── 2. Route to the correct service based on role ────────────────────
        return switch (role.toLowerCase()) {
            case "admin"  -> loginAdmin(email, password, session, res);
            case "client" -> loginClient(email, password, session, res);
            default -> {
                res.put("success", false);
                res.put("message", "Unknown role: '" + role + "'. Expected 'admin' or 'client'.");
                yield ResponseEntity.badRequest().body(res);
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers — each mirrors what the individual controllers do
    // ─────────────────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> loginAdmin(
            String email, String password,
            HttpSession session,
            Map<String, Object> res) {

        try {
            Admin admin = adminService.loginAdmin(email, password);

            // Persist admin identity in server-side session.
            // We set BOTH the unified keys (used by LoginController, BookingController)
            // AND the legacy keys that AdminController.sessionStatus() and verify()
            // still read. Without them the dashboard calls /api/admin/session, sees no
            // "adminId" attribute, returns loggedIn=false, and immediately redirects to login.
            session.setAttribute("userId",        admin.getId());
            session.setAttribute("userName",      admin.getName());
            session.setAttribute("userEmail",     admin.getEmail());
            session.setAttribute("userRole",      "admin");
            session.setAttribute("adminVerified", admin.getVerified());

            // Legacy keys consumed by AdminController.sessionStatus() and verify()
            session.setAttribute("adminId",       admin.getId());
            session.setAttribute("adminName",     admin.getName());
            session.setAttribute("adminEmail",    admin.getEmail());

            res.put("success",  true);
            res.put("role",     "admin");
            res.put("name",     admin.getName());
            res.put("verified", admin.getVerified());

            // Unverified admins must confirm their e-mail PIN first
            res.put("redirect", Boolean.TRUE.equals(admin.getVerified())
                    ? "/admin-dashboard.html"
                    : "/verify.html");

            return ResponseEntity.ok(res);

        } catch (IllegalArgumentException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(401).body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Something went wrong. Please try again.");
            return ResponseEntity.internalServerError().body(res);
        }
    }

    private ResponseEntity<Map<String, Object>> loginClient(
            String email, String password,
            HttpSession session,
            Map<String, Object> res) {

        try {
            Client client = clientService.loginClient(email, password);

            // Persist client identity in server-side session
            session.setAttribute("userId",          client.getId());
            session.setAttribute("userName",        client.getName());
            session.setAttribute("userEmail",       client.getEmail());
            session.setAttribute("userRole",        "client");
            session.setAttribute("clientVerified",  client.getVerified());

            res.put("success",  true);
            res.put("role",     "client");
            res.put("name",     client.getName());
            res.put("verified", client.getVerified());

            // Unverified clients must confirm their e-mail PIN first
            res.put("redirect", Boolean.TRUE.equals(client.getVerified())
                    ? "/client-dashboard.html"
                    : "/client/verify");

            return ResponseEntity.ok(res);

        } catch (IllegalArgumentException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(401).body(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Something went wrong. Please try again.");
            return ResponseEntity.internalServerError().body(res);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/session  — unified session check (used by both dashboards)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> sessionStatus(HttpSession session) {
        Map<String, Object> res = new HashMap<>();

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            res.put("loggedIn", false);
            return ResponseEntity.ok(res);
        }

        res.put("loggedIn", true);
        res.put("id",       userId);
        res.put("name",     session.getAttribute("userName"));
        res.put("email",    session.getAttribute("userEmail"));
        res.put("role",     session.getAttribute("userRole"));

        String role = (String) session.getAttribute("userRole");
        if ("admin".equals(role)) {
            res.put("verified", session.getAttribute("adminVerified"));
        } else {
            res.put("verified", session.getAttribute("clientVerified"));
        }

        return ResponseEntity.ok(res);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/logout  — works for both roles
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of(
                "success",  true,
                "redirect", "/login.html"
        ));
    }
}