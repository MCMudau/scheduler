package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.Admin;
import com.mphoYanga.scheduler.repos.AdminRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AdminSessionController
 * Handles admin session management and passes admin info to frontend pages
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminSessionController {

    @Autowired
    private AdminRepository adminRepository;

    /**
     * Set admin session when navigating from dashboard
     * POST /api/admin/set-session/{adminId}
     * 
     * Frontend calls this when user clicks a link to another page
     * The response contains admin data for the frontend to use
     */
    @PostMapping("/set-session/{adminId}")
    public ResponseEntity<?> setAdminSession(
            @PathVariable Long adminId,
            HttpSession session) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            
            if (adminOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Admin not found", null)
                );
            }

            Admin admin = adminOpt.get();
            
            // Store admin info in session (server-side)
            session.setAttribute("adminId", admin.getId());
            session.setAttribute("adminName", admin.getName());
            session.setAttribute("adminSurname", admin.getSurname());
            session.setAttribute("adminEmail", admin.getEmail());
            
            // Return admin info to frontend (for localStorage fallback)
            Map<String, Object> adminData = new HashMap<>();
            adminData.put("id", admin.getId());
            adminData.put("name", admin.getName());
            adminData.put("surname", admin.getSurname());
            adminData.put("email", admin.getEmail());

            return ResponseEntity.ok(
                new ApiResponse(true, "Admin session set successfully", adminData)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, "Error setting session: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Get current admin session
     * GET /api/admin/current-session
     * 
     * Projects page calls this to retrieve admin info
     */
    @GetMapping("/current-session")
    public ResponseEntity<?> getCurrentAdminSession(HttpSession session) {
        try {
            Long adminId = (Long) session.getAttribute("adminId");
            String adminName = (String) session.getAttribute("adminName");
            String adminSurname = (String) session.getAttribute("adminSurname");
            String adminEmail = (String) session.getAttribute("adminEmail");

            if (adminId == null) {
                return ResponseEntity.status(401).body(
                    new ApiResponse(false, "No admin session found", null)
                );
            }

            Map<String, Object> adminData = new HashMap<>();
            adminData.put("id", adminId);
            adminData.put("name", adminName);
            adminData.put("surname", adminSurname);
            adminData.put("email", adminEmail);

            return ResponseEntity.ok(
                new ApiResponse(true, "Admin session retrieved", adminData)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, "Error retrieving session: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Get admin by ID (for direct API calls)
     * GET /api/admin/{adminId}
     */
    @GetMapping("/{adminId}")
    public ResponseEntity<?> getAdmin(@PathVariable Long adminId) {
        try {
            Optional<Admin> adminOpt = adminRepository.findById(adminId);
            
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                    new ApiResponse(false, "Admin not found", null)
                );
            }

            Admin admin = adminOpt.get();
            Map<String, Object> adminData = new HashMap<>();
            adminData.put("id", admin.getId());
            adminData.put("name", admin.getName());
            adminData.put("surname", admin.getSurname());
            adminData.put("email", admin.getEmail());

            return ResponseEntity.ok(
                new ApiResponse(true, "Admin retrieved", adminData)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, "Error retrieving admin: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Clear admin session (logout)
     * POST /api/admin/clear-session
     */
    @PostMapping("/clear-session")
    public ResponseEntity<?> clearAdminSession(HttpSession session) {
        try {
            session.invalidate();
            return ResponseEntity.ok(
                new ApiResponse(true, "Session cleared successfully", null)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, "Error clearing session: " + e.getMessage(), null)
            );
        }
    }

    // ── API Response Helper Class ─────────────────────────────────

    public static class ApiResponse {
        private Boolean success;
        private String message;
        private Object data;

        public ApiResponse(Boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        // Getters
        public Boolean getSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getData() { return data; }

        // Setters
        public void setSuccess(Boolean success) { this.success = success; }
        public void setMessage(String message) { this.message = message; }
        public void setData(Object data) { this.data = data; }
    }
}
