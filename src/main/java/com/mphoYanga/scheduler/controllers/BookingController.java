package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.Booking;
import com.mphoYanga.scheduler.services.BookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BookingController
 *
 * Base path: /api/bookings
 *
 * ── CLIENT endpoints (client session required) ────────────────────────────
 *   POST   /api/bookings                  → book a slot
 *   GET    /api/bookings/my               → all my bookings (confirmed + cancelled)
 *   GET    /api/bookings/my/active        → only my confirmed bookings
 *   DELETE /api/bookings/{id}             → cancel my booking
 *
 * ── ADMIN endpoints (admin session required) ──────────────────────────────
 *   GET    /api/bookings/admin            → all bookings on my slots
 *   GET    /api/bookings/admin/active     → only confirmed bookings on my slots
 *   GET    /api/bookings/slot/{slotId}    → all confirmed bookings on one slot
 *   DELETE /api/bookings/{id}/admin       → force-cancel any booking on my slots
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // ══════════════════════════════════════════════════════════════
    //  CLIENT — CREATE BOOKING
    //  POST /api/bookings
    //
    //  Body: { "slotId": 5, "service": "plumbing", "notes": "..." }
    //
    //  The client-calendar.html page calls this endpoint when the
    //  user clicks "Confirm Booking" in the modal.
    // ══════════════════════════════════════════════════════════════
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBooking(
            @RequestBody Map<String, Object> body,
            HttpSession session) {

        Map<String, Object> res = new HashMap<>();

        Long clientId = clientIdFromSession(session);
        if (clientId == null) {
            res.put("success", false);
            res.put("message", "You must be logged in as a client to book an appointment.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }

        try {
            Long   slotId  = parseLong(body, "slotId");
            String service = requireString(body, "service");
            String notes   = (String) body.getOrDefault("notes", "");

            Booking booking = bookingService.createBooking(clientId, slotId, service, notes);

            res.put("success", true);
            res.put("message", "Your appointment has been booked successfully.");
            res.put("booking", toMap(booking));
            return ResponseEntity.status(HttpStatus.CREATED).body(res);

        } catch (IllegalArgumentException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);

        } catch (IllegalStateException e) {
            // Slot full / already booked / already cancelled
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(res);

        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Something went wrong. Please try again.");
            return ResponseEntity.internalServerError().body(res);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  CLIENT — GET MY BOOKINGS
    //  GET /api/bookings/my
    //  GET /api/bookings/my/active
    // ══════════════════════════════════════════════════════════════
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyBookings(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        Long clientId = clientIdFromSession(session);
        if (clientId == null) {
            res.put("success", false);
            res.put("message", "Not authenticated.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }
        List<Booking> bookings = bookingService.getMyBookings(clientId);
        res.put("success",  true);
        res.put("bookings", bookings.stream().map(this::toMap).toList());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/my/active")
    public ResponseEntity<Map<String, Object>> getMyActiveBookings(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        Long clientId = clientIdFromSession(session);
        if (clientId == null) {
            res.put("success", false);
            res.put("message", "Not authenticated.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }
        List<Booking> bookings = bookingService.getMyActiveBookings(clientId);
        res.put("success",  true);
        res.put("bookings", bookings.stream().map(this::toMap).toList());
        return ResponseEntity.ok(res);
    }

    // ══════════════════════════════════════════════════════════════
    //  CLIENT — CANCEL MY BOOKING
    //  DELETE /api/bookings/{id}
    //
    //  The client-calendar.html page calls this when "Cancel" is clicked.
    // ══════════════════════════════════════════════════════════════
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable Long id,
            HttpSession session) {

        Map<String, Object> res = new HashMap<>();
        Long clientId = clientIdFromSession(session);
        if (clientId == null) {
            res.put("success", false);
            res.put("message", "Not authenticated.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }
        try {
            Booking cancelled = bookingService.cancelBooking(id, clientId);
            res.put("success", true);
            res.put("message", "Appointment cancelled successfully.");
            res.put("booking", toMap(cancelled));
            return ResponseEntity.ok(res);

        } catch (IllegalArgumentException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);

        } catch (IllegalStateException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(res);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ADMIN — VIEW BOOKINGS ON MY SLOTS
    //  GET /api/bookings/admin
    //  GET /api/bookings/admin/active
    // ══════════════════════════════════════════════════════════════
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> getAdminBookings(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        Long adminId = adminIdFromSession(session);
        if (adminId == null) {
            res.put("success", false);
            res.put("message", "Admin session required.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }
        List<Booking> bookings = bookingService.getBookingsForAdmin(adminId);
        res.put("success",  true);
        res.put("bookings", bookings.stream().map(this::toMap).toList());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/admin/active")
    public ResponseEntity<Map<String, Object>> getActiveAdminBookings(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        Long adminId = adminIdFromSession(session);
        if (adminId == null) {
            res.put("success", false);
            res.put("message", "Admin session required.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }
        List<Booking> bookings = bookingService.getActiveBookingsForAdmin(adminId);
        res.put("success",  true);
        res.put("bookings", bookings.stream().map(this::toMap).toList());
        return ResponseEntity.ok(res);
    }

    // ══════════════════════════════════════════════════════════════
    //  ADMIN — VIEW BOOKINGS ON A SPECIFIC SLOT
    //  GET /api/bookings/slot/{slotId}
    // ══════════════════════════════════════════════════════════════
    @GetMapping("/slot/{slotId}")
    public ResponseEntity<Map<String, Object>> getBookingsForSlot(
            @PathVariable Long slotId,
            HttpSession session) {

        Map<String, Object> res = new HashMap<>();
        Long adminId = adminIdFromSession(session);
        if (adminId == null) {
            res.put("success", false);
            res.put("message", "Admin session required.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }
        List<Booking> bookings = bookingService.getBookingsForSlot(slotId);
        res.put("success",  true);
        res.put("bookings", bookings.stream().map(this::toMap).toList());
        return ResponseEntity.ok(res);
    }

    // ══════════════════════════════════════════════════════════════
    //  ADMIN — FORCE-CANCEL A BOOKING
    //  DELETE /api/bookings/{id}/admin
    // ══════════════════════════════════════════════════════════════
    @DeleteMapping("/{id}/admin")
    public ResponseEntity<Map<String, Object>> adminCancelBooking(
            @PathVariable Long id,
            HttpSession session) {

        Map<String, Object> res = new HashMap<>();
        Long adminId = adminIdFromSession(session);
        if (adminId == null) {
            res.put("success", false);
            res.put("message", "Admin session required.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }
        try {
            Booking cancelled = bookingService.adminCancelBooking(id, adminId);
            res.put("success", true);
            res.put("message", "Booking cancelled by admin.");
            res.put("booking", toMap(cancelled));
            return ResponseEntity.ok(res);

        } catch (IllegalArgumentException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);

        } catch (IllegalStateException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(res);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    /** Extract clientId from session — returns null if not a logged-in client. */
    private Long clientIdFromSession(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (!"client".equals(role)) return null;
        return (Long) session.getAttribute("userId");
    }

    /** Extract adminId from session — returns null if not a logged-in admin. */
    private Long adminIdFromSession(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (!"admin".equals(role)) return null;
        return (Long) session.getAttribute("userId");
    }

    /** Parse a Long from the JSON body safely. */
    private Long parseLong(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null) throw new IllegalArgumentException("Missing required field: " + key);
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for field: " + key);
        }
    }

    /** Require a non-blank String from the JSON body. */
    private String requireString(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null || val.toString().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return val.toString().trim();
    }

    /**
     * Converts a Booking entity to a plain Map for JSON serialisation.
     *
     * We build this manually (rather than relying on Jackson's default
     * serialisation) because the CalendarSlot and Client relations are
     * LAZY-loaded — serialising them directly would either cause a
     * LazyInitializationException or expose fields we don't want.
     */
    private Map<String, Object> toMap(Booking b) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",      b.getId());
        m.put("service", b.getService());
        m.put("notes",   b.getNotes());
        m.put("status",  b.getStatus().name());
        m.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);

        // Include enough slot info for the frontend to display the booking card
        if (b.getSlot() != null) {
            Map<String, Object> slot = new HashMap<>();
            slot.put("id",            b.getSlot().getId());
            slot.put("startDateTime", b.getSlot().getStartDateTime() != null
                    ? b.getSlot().getStartDateTime().toString() : null);
            slot.put("endDateTime",   b.getSlot().getEndDateTime() != null
                    ? b.getSlot().getEndDateTime().toString() : null);
            slot.put("slotType",      b.getSlot().getSlotType() != null
                    ? b.getSlot().getSlotType().name() : null);
            m.put("slot", slot);
        }

        // Include client name for admin views
        if (b.getClient() != null) {
            Map<String, Object> client = new HashMap<>();
            client.put("id",      b.getClient().getId());
            client.put("name",    b.getClient().getName());
            client.put("surname", b.getClient().getSurname());
            client.put("email",   b.getClient().getEmail());
            client.put("phone",   b.getClient().getPhoneNumber());
            m.put("client", client);
        }

        return m;
    }
}
