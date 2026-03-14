package com.mphoYanga.scheduler.services;

import com.mphoYanga.scheduler.models.Admin;
import com.mphoYanga.scheduler.models.CalendarSlot;
import com.mphoYanga.scheduler.models.CalendarSlot.SlotType;
import com.mphoYanga.scheduler.repos.AdminRepository;
import com.mphoYanga.scheduler.repos.CalendarSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class CalendarSlotService {

    private final CalendarSlotRepository slotRepo;
    private final AdminRepository        adminRepo;

    @Autowired
    public CalendarSlotService(CalendarSlotRepository slotRepo,
                               AdminRepository adminRepo) {
        this.slotRepo  = slotRepo;
        this.adminRepo = adminRepo;
    }

    // ══════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════

    /**
     * Create and persist a new calendar slot.
     *
     * The incoming CalendarSlot must have admin.id, startDateTime,
     * endDateTime, and slotType set. Everything else defaults if absent.
     *
     * Validations:
     *   • start must be before end
     *   • start must not be in the past
     *   • window must not overlap an existing slot for the same admin
     *
     * @param slot partially-populated entity from the controller
     * @return the saved slot (now with id, createdAt, updatedAt)
     * @throws IllegalArgumentException on any validation failure
     */
    @Transactional
    public CalendarSlot createSlot(CalendarSlot slot) {

        Admin admin = adminRepo.findById(slot.getAdmin().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Admin not found with id: " + slot.getAdmin().getId()));

        validateTimes(slot.getStartDateTime(), slot.getEndDateTime());
        validateNotInPast(slot.getStartDateTime());
        validateNoOverlapNew(admin.getId(), slot.getStartDateTime(), slot.getEndDateTime());

        // Wire the full admin object (not just the proxy with only an id)
        slot.setAdmin(admin);

        // Apply defaults for optional fields
        if (slot.getMaxBookings()  == null) slot.setMaxBookings(1);
        if (slot.getIsAvailable()  == null) slot.setIsAvailable(true);
        if (slot.getCurrentBookings() == null) slot.setCurrentBookings(0);

        return slotRepo.save(slot);
    }

    // ══════════════════════════════════════════════════════════════
    //  READ — all admins see everything
    // ══════════════════════════════════════════════════════════════

    /**
     * All slots in the system ordered by start time.
     * Used to populate the strip dots on page load.
     */
    public List<CalendarSlot> getAllSlots() {

        List<CalendarSlot> thelist=slotRepo.findAllByOrderByStartDateTimeAsc();
        return  thelist;
    }

    /**
     * All slots for a specific calendar day.
     * Called when any admin clicks a day cell on the strip.
     */
    public List<CalendarSlot> getSlotsForDay(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.atTime(LocalTime.MAX);
        return slotRepo.findAllByDay(start, end);
    }

    /**
     * All slots within a date-time range.
     */
    public List<CalendarSlot> getSlotsInRange(LocalDateTime from, LocalDateTime to) {
        return slotRepo.findAllInRange(from, to);
    }

    /**
     * Single slot by id — for loading the edit modal.
     */
    public Optional<CalendarSlot> getSlotById(Long id) {
        return slotRepo.findById(id);
    }
    // ══════════════════════════════════════════════════════════════
    //  READ — client-facing
    // ══════════════════════════════════════════════════════════════

    /** All slots currently open for a client to book. */
    public List<CalendarSlot> getAvailableSlots() {
        return slotRepo.findAvailableSlots(LocalDateTime.now());
    }

    /** Open slots filtered by type (e.g. client wants a Site Visit). */
    public List<CalendarSlot> getAvailableSlotsByType(SlotType type) {
        return slotRepo.findAvailableSlotsByType(LocalDateTime.now(), type);
    }

    // ══════════════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════════════

    /**
     * Update an existing slot from the edit modal.
     *
     * Only the admin-editable fields are applied from the incoming object:
     *   startDateTime, endDateTime, slotType, maxBookings, isAvailable, notes.
     *
     * currentBookings is NEVER touched here — only confirmBooking /
     * cancelBooking may change that field.
     *
     * @param id      the slot to update
     * @param updated values to apply (controller maps request body into this)
     * @return the saved slot
     * @throws IllegalArgumentException if slot not found or validation fails
     */
    @Transactional
    public CalendarSlot updateSlot(Long id, CalendarSlot updated) {

        CalendarSlot existing = findOrThrow(id);

        validateTimes(updated.getStartDateTime(), updated.getEndDateTime());
        validateNoOverlapExcluding(
                existing.getAdmin().getId(),
                updated.getStartDateTime(),
                updated.getEndDateTime(),
                id
        );

        existing.setStartDateTime(updated.getStartDateTime());
        existing.setEndDateTime(updated.getEndDateTime());

        if (updated.getSlotType()    != null) existing.setSlotType(updated.getSlotType());
        if (updated.getMaxBookings() != null) existing.setMaxBookings(updated.getMaxBookings());
        if (updated.getNotes()       != null) existing.setNotes(updated.getNotes());

        // Respect explicit isAvailable from admin; otherwise re-derive from counts
        if (updated.getIsAvailable() != null) {
            existing.setIsAvailable(updated.getIsAvailable());
        } else {
            existing.setIsAvailable(existing.getCurrentBookings() < existing.getMaxBookings());
        }

        return slotRepo.save(existing);
    }

    /**
     * Quick-toggle isAvailable without opening the full edit modal.
     * The frontend can call this from a toggle button on the timeline block.
     */
    @Transactional
    public CalendarSlot toggleAvailability(Long id) {
        CalendarSlot slot = findOrThrow(id);
        slot.setIsAvailable(!slot.getIsAvailable());
        return slotRepo.save(slot);
    }

    // ══════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════

    /**
     * Delete a slot.
     *
     * If the slot has live bookings and force=false the method throws
     * IllegalStateException so the controller can return 409 CONFLICT,
     * prompting the frontend to show a second confirmation dialog
     * before retrying with force=true.
     *
     * @param id    the slot to delete
     * @param force true = delete even if clients have bookings on this slot
     * @throws IllegalArgumentException if slot not found
     * @throws IllegalStateException    if slot has bookings and force=false
     */
    @Transactional
    public void deleteSlot(Long id, boolean force) {

        CalendarSlot slot = findOrThrow(id);

        if (!force && slot.getCurrentBookings() > 0) {
            throw new IllegalStateException(
                    "This slot has " + slot.getCurrentBookings() +
                            " active booking(s). Send force=true to confirm deletion.");
        }

        slotRepo.deleteById(id);
    }

    // ══════════════════════════════════════════════════════════════
    //  BOOKING INTEGRATION  (called by BookingService)
    // ══════════════════════════════════════════════════════════════

    /**
     * Increment currentBookings when a client confirms a booking.
     * Auto-closes the slot when it reaches capacity.
     *
     * Called by BookingService — NOT by the calendar controller.
     *
     * @throws IllegalStateException if the slot is closed or already full
     */
    @Transactional
    public CalendarSlot confirmBooking(Long slotId) {

        CalendarSlot slot = findOrThrow(slotId);

        if (!slot.getIsAvailable()) {
            throw new IllegalStateException("Slot is not available for booking.");
        }
        if (slot.getCurrentBookings() >= slot.getMaxBookings()) {
            throw new IllegalStateException("Slot is fully booked.");
        }

        slot.setCurrentBookings(slot.getCurrentBookings() + 1);

        // Auto-close when full
        if (slot.getCurrentBookings() >= slot.getMaxBookings()) {
            slot.setIsAvailable(false);
        }

        return slotRepo.save(slot);
    }

    /**
     * Decrement currentBookings when a client cancels.
     * Auto-reopens the slot if it was closed because it was full.
     *
     * Called by BookingService on cancellation.
     */
    @Transactional
    public CalendarSlot cancelBooking(Long slotId) {

        CalendarSlot slot = findOrThrow(slotId);

        if (slot.getCurrentBookings() <= 0) {
            throw new IllegalStateException("No bookings to cancel on this slot.");
        }

        slot.setCurrentBookings(slot.getCurrentBookings() - 1);

        // Re-open if it was auto-closed due to being full
        if (slot.getCurrentBookings() < slot.getMaxBookings()) {
            slot.setIsAvailable(true);
        }

        return slotRepo.save(slot);
    }

    // ══════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════

    private CalendarSlot findOrThrow(Long id) {
        return slotRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Calendar slot not found with id: " + id));
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end date/time are required.");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start date/time must be before end date/time.");
        }
    }

    private void validateNotInPast(LocalDateTime start) {
        if (start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot create a slot in the past.");
        }
    }

    /**
     * Overlap check for a brand-new slot.
     * Throws with the conflicting slot's type and time in the message
     * so the admin knows exactly what they are clashing with.
     */
    private void validateNoOverlapNew(Long adminId, LocalDateTime start, LocalDateTime end) {
        List<CalendarSlot> conflicts = slotRepo.findOverlappingSlots(adminId, start, end);
        if (!conflicts.isEmpty()) {
            CalendarSlot clash = conflicts.get(0);
            throw new IllegalArgumentException(
                    "This time window overlaps an existing " +
                            clash.getSlotType().name() + " slot (" +
                            clash.getStartDateTime().toLocalTime() + " – " +
                            clash.getEndDateTime().toLocalTime() +
                            "). Please choose a different time.");
        }
    }

    /**
     * Overlap check when editing an existing slot.
     * Excludes the slot being edited so it does not conflict with itself.
     */
    private void validateNoOverlapExcluding(Long adminId, LocalDateTime start,
                                            LocalDateTime end, Long excludeId) {
        List<CalendarSlot> conflicts = slotRepo.findOverlappingSlotsExcluding(adminId, excludeId, start, end);
        if (!conflicts.isEmpty()) {
            CalendarSlot clash = conflicts.get(0);
            throw new IllegalArgumentException(
                    "This time window overlaps an existing " +
                            clash.getSlotType().name() + " slot (" +
                            clash.getStartDateTime().toLocalTime() + " – " +
                            clash.getEndDateTime().toLocalTime() +
                            "). Please choose a different time.");
        }
    }
}