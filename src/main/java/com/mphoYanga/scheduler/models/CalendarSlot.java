package com.mphoYanga.scheduler.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * CalendarSlot
 *
 * Represents a time window that the admin opens for client bookings.
 *
 * State machine:
 *   OPEN       → isAvailable=true,  currentBookings < maxBookings
 *   FULL       → isAvailable=false, currentBookings == maxBookings  (auto-closed by service)
 *   BLOCKED    → isAvailable=false, currentBookings == 0            (manually closed by admin)
 *   CANCELLED  → isAvailable=false, currentBookings > 0             (admin force-closed a live slot)
 */
@Entity
@Table(name = "calendar_slots")
public class CalendarSlot {

    // ── IDENTITY ──────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── RELATIONSHIPS ─────────────────────────────────────────────

    /**
     * The admin who owns this slot.
     * Many slots → one admin.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    // ── TIME ──────────────────────────────────────────────────────

    /**
     * When the slot starts.
     * Cannot be null; must be before endDateTime.
     */
    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    /**
     * When the slot ends.
     * Cannot be null; must be after startDateTime.
     */
    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    // ── AVAILABILITY ─────────────────────────────────────────────

    /**
     * Master availability switch.
     *
     * true  → slot is open and visible to clients for booking.
     * false → slot is closed (blocked by admin, fully booked, or cancelled).
     *
     * The service auto-sets this to false when currentBookings reaches maxBookings.
     * The service auto-sets this back to true when a cancellation brings
     * currentBookings below maxBookings.
     */
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    /**
     * Maximum number of clients allowed to book this slot concurrently.
     * Default 1 (one-on-one appointment).
     * Set higher for group consultations or open-house events.
     */
    @Column(name = "max_bookings", nullable = false)
    private Integer maxBookings = 1;

    /**
     * How many clients have booked this slot so far.
     * Incremented by the booking service on each confirmed booking.
     * Decremented on cancellation.
     * Never exceeds maxBookings.
     */
    @Column(name = "current_bookings", nullable = false)
    private Integer currentBookings = 0;

    // ── CLASSIFICATION ────────────────────────────────────────────

    /**
     * The purpose / nature of this time slot.
     *
     * CONSULTATION  — initial client meeting (office or virtual)
     * SITE_VISIT    — travel to client's property for assessment
     * QUOTATION     — formal quote presentation meeting
     * FOLLOW_UP     — progress review with an existing client
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type", nullable = false, length = 30)
    private SlotType slotType;

    public enum SlotType {
        CONSULTATION,
        SITE_VISIT,
        QUOTATION,
        FOLLOW_UP
    }

    // ── METADATA ──────────────────────────────────────────────────

    /**
     * Optional free-text notes visible only to the admin.
     * e.g. "Client requested WhatsApp call instead of in-person."
     */
    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── LIFECYCLE HOOKS ───────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── CONSTRUCTORS ──────────────────────────────────────────────

    public CalendarSlot() {}

    public CalendarSlot(Admin admin,
                        LocalDateTime startDateTime,
                        LocalDateTime endDateTime,
                        SlotType slotType,
                        Integer maxBookings,
                        Boolean isAvailable,
                        String notes) {
        this.admin         = admin;
        this.startDateTime = startDateTime;
        this.endDateTime   = endDateTime;
        this.slotType      = slotType;
        this.maxBookings   = maxBookings;
        this.isAvailable   = isAvailable;
        this.currentBookings = 0;
        this.notes         = notes;
    }

    // ── DERIVED HELPERS ───────────────────────────────────────────

    /** Returns true if the slot still has room for at least one more booking. */
    public boolean hasCapacity() {
        return isAvailable && currentBookings < maxBookings;
    }

    /** Returns the number of remaining open spaces. */
    public int remainingCapacity() {
        return Math.max(0, maxBookings - currentBookings);
    }

    // ── GETTERS & SETTERS ─────────────────────────────────────────

    public Long getId()                          { return id; }

    public Admin getAdmin()                      { return admin; }
    public void  setAdmin(Admin admin)           { this.admin = admin; }

    public LocalDateTime getStartDateTime()                        { return startDateTime; }
    public void          setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }

    public LocalDateTime getEndDateTime()                        { return endDateTime; }
    public void          setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }

    public Boolean getIsAvailable()                    { return isAvailable; }
    public void    setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public Integer getMaxBookings()                    { return maxBookings; }
    public void    setMaxBookings(Integer maxBookings) { this.maxBookings = maxBookings; }

    public Integer getCurrentBookings()                        { return currentBookings; }
    public void    setCurrentBookings(Integer currentBookings) { this.currentBookings = currentBookings; }

    public SlotType getSlotType()                { return slotType; }
    public void     setSlotType(SlotType slotType) { this.slotType = slotType; }

    public String getNotes()             { return notes; }
    public void   setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
