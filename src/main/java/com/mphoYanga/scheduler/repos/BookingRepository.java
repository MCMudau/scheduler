package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.Booking;
import com.mphoYanga.scheduler.models.Booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ── BY CLIENT ─────────────────────────────────────────────────

    /** All bookings for a client, newest first. */
    List<Booking> findByClientIdOrderByCreatedAtDesc(Long clientId);

    /** Active (non-cancelled) bookings for a client. */
    List<Booking> findByClientIdAndStatusOrderByCreatedAtDesc(Long clientId, BookingStatus status);

    // ── BY SLOT ───────────────────────────────────────────────────

    /** All active bookings on a specific slot. */
    List<Booking> findBySlotIdAndStatus(Long slotId, BookingStatus status);

    /** Count of active bookings on a slot — used for capacity checks. */
    long countBySlotIdAndStatus(Long slotId, BookingStatus status);

    // ── DUPLICATE GUARD ───────────────────────────────────────────

    /**
     * Check whether a client already has a CONFIRMED booking on a given slot.
     * Prevents a client from booking the same slot twice.
     */
    boolean existsByClientIdAndSlotIdAndStatus(Long clientId, Long slotId, BookingStatus status);

    // ── BY ADMIN (for admin dashboard) ───────────────────────────

    /**
     * All bookings on slots owned by a specific admin.
     * Allows the admin to see who booked their slots.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.slot.admin.id = :adminId
        ORDER BY b.slot.startDateTime ASC
        """)
    List<Booking> findByAdminId(@Param("adminId") Long adminId);

    /**
     * Active bookings on slots owned by a specific admin.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.slot.admin.id = :adminId
          AND b.status        = :status
        ORDER BY b.slot.startDateTime ASC
        """)
    List<Booking> findByAdminIdAndStatus(
            @Param("adminId") Long          adminId,
            @Param("status")  BookingStatus status);

    // ── LOOKUP ────────────────────────────────────────────────────

    /**
     * Find a booking by id, confirming it belongs to the given client.
     * Used for cancel — prevents a client cancelling someone else's booking.
     */
    Optional<Booking> findByIdAndClientId(Long id, Long clientId);
}
