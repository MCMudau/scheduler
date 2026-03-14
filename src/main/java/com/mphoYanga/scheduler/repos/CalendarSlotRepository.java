package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.CalendarSlot;
import com.mphoYanga.scheduler.models.CalendarSlot.SlotType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarSlotRepository extends JpaRepository<CalendarSlot, Long> {

    // ── ALL SLOTS (all admins) ────────────────────────────────────

    /** Every slot in the system, ordered by start time. Used to populate strip dots. */
    List<CalendarSlot> findAllByOrderByStartDateTimeAsc();

    // ── SLOTS FOR A SPECIFIC DAY (all admins) ─────────────────────

    /**
     * All slots on a given day regardless of which admin created them.
     * Caller passes dayStart = midnight, dayEnd = 23:59:59.999.
     */
    @Query("""
        SELECT s FROM CalendarSlot s
        WHERE s.startDateTime >= :dayStart
          AND s.startDateTime <  :dayEnd
        ORDER BY s.startDateTime ASC
        """)
    List<CalendarSlot> findAllByDay(
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd")   LocalDateTime dayEnd
    );

    // ── SLOTS IN A DATE RANGE (all admins) ────────────────────────

    /**
     * All slots that overlap [from, to] regardless of which admin created them.
     * Used to load the strip for a visible month or week.
     */
    @Query("""
        SELECT s FROM CalendarSlot s
        WHERE s.startDateTime < :to
          AND s.endDateTime   > :from
        ORDER BY s.startDateTime ASC
        """)
    List<CalendarSlot> findAllInRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );

    // ── OPEN SLOTS FOR CLIENT BOOKING ─────────────────────────────

    /**
     * Slots currently available for a client to book:
     *   isAvailable = true, not yet full, start is in the future.
     */
    @Query("""
        SELECT s FROM CalendarSlot s
        WHERE s.isAvailable     = true
          AND s.currentBookings < s.maxBookings
          AND s.startDateTime   > :now
        ORDER BY s.startDateTime ASC
        """)
    List<CalendarSlot> findAvailableSlots(@Param("now") LocalDateTime now);

    /**
     * Open slots filtered by type.
     * Used when a client selects what kind of appointment they want.
     */
    @Query("""
        SELECT s FROM CalendarSlot s
        WHERE s.isAvailable     = true
          AND s.currentBookings < s.maxBookings
          AND s.startDateTime   > :now
          AND s.slotType        = :type
        ORDER BY s.startDateTime ASC
        """)
    List<CalendarSlot> findAvailableSlotsByType(
            @Param("now")  LocalDateTime now,
            @Param("type") SlotType      type
    );

    // ── OVERLAP DETECTION (scoped to the creating admin) ─────────

    /**
     * Overlap check for a new slot being created by a specific admin.
     * Each admin's slots must not overlap each other — but two different
     * admins CAN have slots at the same time (they are separate people).
     */
    @Query("""
        SELECT s FROM CalendarSlot s
        WHERE s.admin.id      = :adminId
          AND s.startDateTime < :end
          AND s.endDateTime   > :start
        ORDER BY s.startDateTime ASC
        """)
    List<CalendarSlot> findOverlappingSlots(
            @Param("adminId") Long          adminId,
            @Param("start")   LocalDateTime start,
            @Param("end")     LocalDateTime end
    );

    /**
     * Same overlap check but excludes a specific slot ID.
     * Used when editing so a slot doesn't conflict with itself.
     */
    @Query("""
        SELECT s FROM CalendarSlot s
        WHERE s.admin.id      = :adminId
          AND s.id           != :excludeId
          AND s.startDateTime < :end
          AND s.endDateTime   > :start
        ORDER BY s.startDateTime ASC
        """)
    List<CalendarSlot> findOverlappingSlotsExcluding(
            @Param("adminId")   Long          adminId,
            @Param("excludeId") Long          excludeId,
            @Param("start")     LocalDateTime start,
            @Param("end")       LocalDateTime end
    );
}