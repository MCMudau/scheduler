package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.Admin;
import com.mphoYanga.scheduler.models.CalendarSlot;
import com.mphoYanga.scheduler.models.CalendarSlot.SlotType;
import com.mphoYanga.scheduler.services.AdminService;
import com.mphoYanga.scheduler.services.CalendarSlotService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CalendarSlotController
 *
 * Base path: /api/calendar/slots
 *
 * Admin endpoints (session required):
 *   POST   /                            → create a slot
 *   GET    /admin/{adminId}             → all slots for an admin
 *   GET    /admin/{adminId}/day         → slots for one day   ?date=2026-03-01
 *   GET    /admin/{adminId}/range       → slots in range      ?from=...&to=...
 *   GET    /{id}                        → single slot (edit modal)
 *   PUT    /{id}                        → update a slot
 *   PATCH  /{id}/toggle                 → flip isAvailable on/off
 *   DELETE /{id}                        → delete a slot       ?force=false
 *
 * Public endpoints (no session required):
 *   GET    /available                   → all open slots (client booking page)
 *   GET    /available/type/{type}       → open slots by SlotType
 */
@RestController
@RequestMapping("/api/calendar/slots")
public class CalendarSlotController {

    private final CalendarSlotService slotService;
    @Autowired
    private AdminService adminService;

    @Autowired
    public CalendarSlotController(CalendarSlotService slotService) {
        this.slotService = slotService;
    }

    // ══════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════

    /**
     * POST /api/calendar/slots
     *
     * Request body — CalendarSlot JSON (admin.id required):
     * {
     *   "admin":          { "id": 1 },
     *   "startDateTime":  "2026-03-10T09:00:00",
     *   "endDateTime":    "2026-03-10T10:00:00",
     *   "slotType":       "CONSULTATION",
     *   "maxBookings":    1,
     *   "isAvailable":    true,
     *   "notes":          "optional"
     * }
     */
    @PostMapping
    public ResponseEntity<?> createSlot(@RequestBody CalendarSlot slot,
                                        HttpSession session) {
        if (!adminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be logged in as admin.");
        }
        try {
            addAdmin(slot);
            CalendarSlot saved = slotService.createSlot(slot);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private void addAdmin(CalendarSlot slot) {
        Admin admin= adminService.findById(id).get();
        slot.setAdmin(admin);
    }

    // ══════════════════════════════════════════════════════════════
    //  READ — admin
    // ══════════════════════════════════════════════════════════════

    /**
     * GET /api/calendar/slots/admin/{adminId}
     *
     * All slots for an admin ordered by start time.
     * Called on calendar page load to know which days have dots.
     */
    @GetMapping("/admin/{adminId}")
    public ResponseEntity<?> getAllSlots(@PathVariable Long adminId,
                                         HttpSession session) {
        if (!adminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.");
        }
        List<CalendarSlot> slots = slotService.getAllSlots();
        return ResponseEntity.ok(slots);
    }

    /**
     * GET /api/calendar/slots/admin/{adminId}/day?date=2026-03-01
     *
     * Slots for a specific day — called when admin clicks a day cell.
     */
    @GetMapping("/admin/day")
    public ResponseEntity<?> getSlotsForDay(
            @PathVariable Long adminId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {

        if (!adminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.");
        }
        List<CalendarSlot> slots = slotService.getSlotsForDay( date);
        return ResponseEntity.ok(slots);
    }

    /**
     * GET /api/calendar/slots/admin/{adminId}/range
     *       ?from=2026-03-01T00:00:00&to=2026-03-31T23:59:59
     *
     * Slots within a date-time range (loading a visible month or week).
     */
    @GetMapping("/admin/{adminId}/range")
    public ResponseEntity<?> getSlotsInRange(
            @PathVariable Long adminId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            HttpSession session) {

        if (!adminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.");
        }
        List<CalendarSlot> slots = slotService.getSlotsInRange(from, to);
        return ResponseEntity.ok(slots);
    }

    /**
     * GET /api/calendar/slots/{id}
     *
     * Single slot by id — called when the admin opens the edit modal.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSlotById(@PathVariable Long id,
                                          HttpSession session) {
        if (!adminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.");
        }
        return slotService.getSlotById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Calendar slot not found with id: " + id));
    }

    // ══════════════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════════════

    /**
     * PUT /api/calendar/slots/{id}
     *
     * Full update from the edit modal.
     * Send the same CalendarSlot JSON shape as on create.
     * currentBookings in the body is ignored — the service never
     * lets an edit touch that field.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSlot(@PathVariable Long id,
                                         @RequestBody CalendarSlot slot,
                                         HttpSession session) {
        if (!adminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.");
        }
        try {
            CalendarSlot updated = slotService.updateSlot(id, slot);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * PATCH /api/calendar/slots/{id}/toggle
     *
     * Flip isAvailable without opening the full edit modal.
     * The frontend calls this from the quick-toggle button on a timeline block.
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleAvailability(@PathVariable Long id,
                                                 HttpSession session) {
        if (!adminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.");
        }
        try {
            CalendarSlot updated = slotService.toggleAvailability(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════

    /**
     * DELETE /api/calendar/slots/{id}?force=false
     *
     * Two-step delete for slots that have live bookings:
     *
     *   First call  (force=false, the default)
     *     → If currentBookings > 0 the service throws IllegalStateException.
     *       The controller returns 409 CONFLICT with a message.
     *       The frontend catches 409, shows a second "clients are booked —
     *       are you sure?" dialog, then retries with force=true.
     *
     *   Second call (force=true)
     *     → Slot is deleted regardless of bookings.
     *       Your future BookingService should handle notifying those clients.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSlot(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force,
            HttpSession session) {

        if (!adminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.");
        }
        try {
            slotService.deleteSlot(id, force);
            return ResponseEntity.noContent().build(); // 204
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            // Has live bookings — prompt frontend to confirm with force=true
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC — client booking page
    // ══════════════════════════════════════════════════════════════

    /**
     * GET /api/calendar/slots/available
     *
     * All slots open for a client to book right now.
     * No session required — this endpoint is public.
     */
    @GetMapping("/available")
    public ResponseEntity<List<CalendarSlot>> getAvailableSlots() {
        return ResponseEntity.ok(slotService.getAvailableSlots());
    }

    /**
     * GET /api/calendar/slots/available/type/{type}
     *
     * Open slots filtered by SlotType.
     * e.g. GET /available/type/SITE_VISIT
     */
    @GetMapping("/available/type/{type}")
    public ResponseEntity<?> getAvailableByType(@PathVariable SlotType type) {
        List<CalendarSlot> slots = slotService.getAvailableSlotsByType(type);
        return ResponseEntity.ok(slots);
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPER
    // ══════════════════════════════════════════════════════════════

    Long id;
    private boolean adminLoggedIn(HttpSession session) {
        id=(Long) session.getAttribute("adminId");
        return session.getAttribute("adminId") != null;
    }

    /**
     * GET /api/calendar/slots/all
     *
     * Returns every slot in the system ordered by start time.
     * Used on page load to paint dots on the calendar strip.
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllSlots(HttpSession session) {
        if (adminLoggedIn(session) == false) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.");
        return ResponseEntity.ok(slotService.getAllSlots());
    }

    /**
     * GET /api/calendar/slots/day?date=2026-03-01
     *
     * All slots on a specific day. Called when any admin clicks a day cell.
     */
    @GetMapping("/day")
    public ResponseEntity<?> getSlotsForDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {

        if (adminLoggedIn(session) == false) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.");
        return ResponseEntity.ok(slotService.getSlotsForDay(date));
    }

}
