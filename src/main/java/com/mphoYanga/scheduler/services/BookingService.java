package com.mphoYanga.scheduler.services;

import com.mphoYanga.scheduler.models.Booking;
import com.mphoYanga.scheduler.models.Booking.BookingStatus;
import com.mphoYanga.scheduler.models.CalendarSlot;
import com.mphoYanga.scheduler.models.Client;
import com.mphoYanga.scheduler.repos.BookingRepository;
import com.mphoYanga.scheduler.repos.CalendarSlotRepository;
import com.mphoYanga.scheduler.repos.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository     bookingRepository;
    private final CalendarSlotRepository slotRepository;
    private final ClientRepository      clientRepository;

    @Autowired
    public BookingService(BookingRepository bookingRepository,
                          CalendarSlotRepository slotRepository,
                          ClientRepository clientRepository) {
        this.bookingRepository = bookingRepository;
        this.slotRepository    = slotRepository;
        this.clientRepository  = clientRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE BOOKING
    //
    // Called by: POST /api/bookings
    // Guards:
    //   1. Client must exist
    //   2. Slot must exist
    //   3. Slot must be in the future
    //   4. Slot must be available and have capacity
    //   5. Client must not have already booked this slot
    // Side-effect:
    //   - Increments slot.currentBookings
    //   - Auto-closes slot (isAvailable=false) when it reaches maxBookings
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public Booking createBooking(Long clientId, Long slotId, String service, String notes) {

        // ── 1. Resolve client ─────────────────────────────────────
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found."));

        // ── 2. Resolve slot ───────────────────────────────────────
        CalendarSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found."));

        // ── 3. Slot must be in the future ─────────────────────────
        if (!slot.getStartDateTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("This slot has already passed and can no longer be booked.");
        }

        // ── 4. Slot must be available and have capacity ───────────
        if (!Boolean.TRUE.equals(slot.getIsAvailable())) {
            throw new IllegalStateException("This slot is no longer available.");
        }
        if (slot.getCurrentBookings() >= slot.getMaxBookings()) {
            throw new IllegalStateException("This slot is fully booked.");
        }

        // ── 5. Prevent duplicate booking ──────────────────────────
        boolean alreadyBooked = bookingRepository.existsByClientIdAndSlotIdAndStatus(
                clientId, slotId, BookingStatus.CONFIRMED);
        if (alreadyBooked) {
            throw new IllegalStateException("You already have a booking for this slot.");
        }

        // ── 6. Validate service field ─────────────────────────────
        if (service == null || service.isBlank()) {
            throw new IllegalArgumentException("Please select a service.");
        }

        // ── 7. Persist booking ────────────────────────────────────
        Booking booking = new Booking(client, slot, service.trim(),
                notes != null ? notes.trim() : "");
        bookingRepository.save(booking);

        // ── 8. Update slot capacity ───────────────────────────────
        slot.setCurrentBookings(slot.getCurrentBookings() + 1);
        if (slot.getCurrentBookings() >= slot.getMaxBookings()) {
            slot.setIsAvailable(false);   // auto-close when full
        }
        slotRepository.save(slot);

        return booking;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL BOOKING (by client)
    //
    // Called by: DELETE /api/bookings/{id}  (client session required)
    // Guards:
    //   - Booking must belong to the requesting client
    //   - Booking must be CONFIRMED (not already cancelled)
    // Side-effect:
    //   - Sets booking.status = CANCELLED
    //   - Decrements slot.currentBookings
    //   - Re-opens slot (isAvailable=true) if it was auto-closed
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public Booking cancelBooking(Long bookingId, Long clientId) {

        Booking booking = bookingRepository.findByIdAndClientId(bookingId, clientId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found or does not belong to you."));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("This booking has already been cancelled.");
        }

        // Mark cancelled
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Restore slot capacity
        CalendarSlot slot = booking.getSlot();
        int updated = Math.max(0, slot.getCurrentBookings() - 1);
        slot.setCurrentBookings(updated);

        // Re-open slot if it was auto-closed due to being full
        if (updated < slot.getMaxBookings()) {
            slot.setIsAvailable(true);
        }
        slotRepository.save(slot);

        return booking;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL BOOKING (by admin — force cancel any booking on their slot)
    //
    // Called by: DELETE /api/bookings/{id}/admin  (admin session required)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public Booking adminCancelBooking(Long bookingId, Long adminId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        // Verify the booking is on one of this admin's slots
        if (!booking.getSlot().getAdmin().getId().equals(adminId)) {
            throw new IllegalArgumentException("You do not own the slot for this booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("This booking is already cancelled.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        CalendarSlot slot = booking.getSlot();
        int updated = Math.max(0, slot.getCurrentBookings() - 1);
        slot.setCurrentBookings(updated);
        if (updated < slot.getMaxBookings()) {
            slot.setIsAvailable(true);
        }
        slotRepository.save(slot);

        return booking;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY — client views their own bookings
    // ─────────────────────────────────────────────────────────────────────────

    /** All bookings for the client (confirmed + cancelled), newest first. */
    public List<Booking> getMyBookings(Long clientId) {
        return bookingRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    /** Only active (CONFIRMED) bookings for the client. */
    public List<Booking> getMyActiveBookings(Long clientId) {
        return bookingRepository.findByClientIdAndStatusOrderByCreatedAtDesc(
                clientId, BookingStatus.CONFIRMED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY — admin views bookings on their slots
    // ─────────────────────────────────────────────────────────────────────────

    /** All bookings on slots owned by the given admin. */
    public List<Booking> getBookingsForAdmin(Long adminId) {
        return bookingRepository.findByAdminId(adminId);
    }

    /** Only active bookings on slots owned by the given admin. */
    public List<Booking> getActiveBookingsForAdmin(Long adminId) {
        return bookingRepository.findByAdminIdAndStatus(adminId, BookingStatus.CONFIRMED);
    }

    /** All bookings on a specific slot (admin use). */
    public List<Booking> getBookingsForSlot(Long slotId) {
        return bookingRepository.findBySlotIdAndStatus(slotId, BookingStatus.CONFIRMED);
    }
}
