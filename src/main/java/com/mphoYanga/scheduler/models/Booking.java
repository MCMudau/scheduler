package com.mphoYanga.scheduler.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Booking
 *
 * Represents a client's confirmed reservation of a CalendarSlot.
 *
 * Lifecycle:
 *   CONFIRMED  → created successfully, slot capacity decremented
 *   CANCELLED  → cancelled by client or admin, slot capacity restored
 */
@Entity
@Table(name = "bookings")
public class Booking {

    // ── IDENTITY ──────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── RELATIONSHIPS ─────────────────────────────────────────────

    /** The client who made this booking. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** The slot that was booked. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private CalendarSlot slot;

    // ── BOOKING DETAILS ───────────────────────────────────────────

    /**
     * The service the client needs.
     * e.g. "plumbing", "tiling", "painting", "ceiling", "renovation", "quote"
     */
    @Column(name = "service", nullable = false, length = 60)
    private String service;

    /** Optional free-text from the client describing their project. */
    @Column(name = "notes", length = 1000)
    private String notes;

    // ── STATUS ────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status = BookingStatus.CONFIRMED;

    public enum BookingStatus {
        CONFIRMED,
        CANCELLED
    }

    // ── METADATA ──────────────────────────────────────────────────

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

    public Booking() {}

    public Booking(Client client, CalendarSlot slot, String service, String notes) {
        this.client  = client;
        this.slot    = slot;
        this.service = service;
        this.notes   = notes;
        this.status  = BookingStatus.CONFIRMED;
    }

    // ── GETTERS & SETTERS ─────────────────────────────────────────

    public Long          getId()                        { return id; }

    public Client        getClient()                    { return client; }
    public void          setClient(Client client)       { this.client = client; }

    public CalendarSlot  getSlot()                      { return slot; }
    public void          setSlot(CalendarSlot slot)     { this.slot = slot; }

    public String        getService()                   { return service; }
    public void          setService(String service)     { this.service = service; }

    public String        getNotes()                     { return notes; }
    public void          setNotes(String notes)         { this.notes = notes; }

    public BookingStatus getStatus()                    { return status; }
    public void          setStatus(BookingStatus status){ this.status = status; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public LocalDateTime getUpdatedAt()                 { return updatedAt; }
}
