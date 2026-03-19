package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.Admin;
import com.mphoYanga.scheduler.services.AdminService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/admin/register
    //
    // Matches the fetch call in register.html:
    //   body: JSON.stringify({ name, surname, email, phoneNumber, password, confirmPassword })
    // ─────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerAdmin(
            @RequestBody Map<String, String> body) {

        Map<String, Object> res = new HashMap<>();

        String name            = body.get("name");
        String surname         = body.get("surname");
        String email           = body.get("email");
        String phoneNumber     = body.get("phoneNumber");
        String password        = body.get("password");
        String confirmPassword = body.get("confirmPassword");

        // Basic null/blank guard — frontend validates too, but defence in depth
        if (name == null || surname == null || email == null ||
                phoneNumber == null || password == null || confirmPassword == null ||
                name.isBlank() || surname.isBlank() || email.isBlank() ||
                phoneNumber.isBlank() || password.isBlank() || confirmPassword.isBlank()) {

            res.put("success", false);
            res.put("message", "All fields are required.");
            return ResponseEntity.badRequest().body(res);
        }

        try {
            Admin admin = adminService.registerAdmin(
                    name, surname, email, phoneNumber, password, confirmPassword
            );

            res.put("success", true);
            res.put("message", "Account created successfully. A verification PIN has been sent to your email.");
            res.put("adminId", admin.getId());
            return ResponseEntity.ok(res);

        } catch (IllegalArgumentException e) {
            // Covers: passwords don't match, email already taken
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);

        } catch (Exception e) {
            // Covers: email sending failure or any unexpected error
            res.put("success", false);
            res.put("message", "Something went wrong. Please try again."+e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/admin/login
    //
    // Accepts: { "email": "...", "password": "..." }
    // On success: saves session, returns redirect target
    // ─────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginAdmin(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        Map<String, Object> res = new HashMap<>();

        String email    = body.get("email");
        String password = body.get("password");

        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            res.put("success", false);
            res.put("message", "Email and password are required.");
            return ResponseEntity.badRequest().body(res);
        }

        try {
            Admin admin = adminService.loginAdmin(email, password);

            // Store admin info in server-side session
            session.setAttribute("adminId",       admin.getId());
            session.setAttribute("adminName",     admin.getName());
            session.setAttribute("adminEmail",    admin.getEmail());
            session.setAttribute("adminVerified", admin.getVerified());

            res.put("success",  true);
            res.put("name",     admin.getName());
            res.put("verified", admin.getVerified());

            // Unverified admins must enter their PIN before accessing the dashboard
            res.put("redirect", admin.getVerified()
                    ? "/admin-dashboard.html"
                    : "/verify.html");

            return ResponseEntity.ok(res);

        } catch (IllegalArgumentException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(401).body(res);
        }
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/admin/verify
    //
    // Accepts: { "pin": "12345" }
    // Requires an active session from a prior login
    // ─────────────────────────────────────────────────────────
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyAdmin(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        Map<String, Object> res = new HashMap<>();

        Long adminId = (Long) session.getAttribute("userId");
        if (adminId == null) {
            res.put("success", false);
            res.put("message", "Session expired. Please log in again.");
            res.put("redirect", "/login");
            return ResponseEntity.status(401).body(res);
        }

        String pin = body.get("pin");
        if (pin == null || pin.isBlank()) {
            res.put("success", false);
            res.put("message", "Please enter your verification PIN.");
            return ResponseEntity.badRequest().body(res);
        }

        try {
            Admin admin = adminService.verifyAdmin(adminId, pin);

            session.setAttribute("adminVerified", true);

            res.put("success",  true);
            res.put("message",  "Account verified successfully!");
            res.put("redirect", "/admin-dashboard.html");
            return ResponseEntity.ok(res);

        } catch (IllegalArgumentException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(400).body(res);
        }
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/admin/logout
    // ─────────────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of(
                "success",  true,
                "redirect", "/"
        ));
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/admin/session
    // Frontend calls this on page load to check login state
    // ─────────────────────────────────────────────────────────
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> sessionStatus(HttpSession session) {
        Map<String, Object> res = new HashMap<>();

        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            res.put("loggedIn", false);
            return ResponseEntity.ok(res);
        }

        res.put("loggedIn",  true);
        res.put("name",      session.getAttribute("adminName"));
        res.put("email",     session.getAttribute("adminEmail"));
        res.put("verified",  session.getAttribute("adminVerified"));
        res.put("role",      "admin");
        return ResponseEntity.ok(res);
    }
}