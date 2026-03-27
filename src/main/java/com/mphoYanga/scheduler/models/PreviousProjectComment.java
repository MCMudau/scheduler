package com.mphoYanga.scheduler.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A comment left by a registered client on a {@link PreviousProject}.
 *
 * Ownership rule: one PreviousProjectComment belongs to exactly one PreviousProject.
 *
 * Comment authorship: comments are linked to a {@link Client} via a nullable
 * foreign key. Keeping it nullable means that if a client account is ever
 * deleted the comment row is preserved (author shown as "Former Client")
 * rather than being cascade-deleted — this preserves the social proof value
 * of the comment for Mpho Yanga.
 *
 * Moderation: the {@code approved} flag lets admins review comments before
 * they appear publicly. New comments default to {@code approved = false}.
 */
@Entity
@Table(name = "previous_project_comments")
public class PreviousProjectComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Parent relationship ───────────────────────────────────────────────────

    /**
     * The project this comment belongs to.
     * {@code @JsonBackReference} suppresses this side during serialisation
     * to break the circular loop with {@code PreviousProject.comments}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "previous_project_id", nullable = false)
    @JsonBackReference("project-comments")
    private PreviousProject previousProject;

    // ── Author reference ──────────────────────────────────────────────────────

    /**
     * The client who wrote this comment.
     * Nullable: if the client account is deleted the comment is kept but
     * {@code client} will be null. The service layer should fall back to
     * displaying {@code authorDisplayName} in that case.
     *
     * CascadeType is intentionally omitted — the Client entity is managed
     * independently; we must not cascade deletes here.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "client_id", nullable = true)
    private Client client;

    /**
     * Denormalised display name captured at comment time.
     * Stored separately so the author's name still renders correctly even
     * if the client updates their name or their account is deleted.
     * Set automatically by the service layer from {@code client.getName() + " " + client.getSurname()}.
     */
    @Column(nullable = false, length = 200)
    private String authorDisplayName;

    // ── Content ───────────────────────────────────────────────────────────────

    /** The comment body — free-form text from the client. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Optional star rating (1–5) accompanying the comment.
     * Null means the client left a text comment without a rating.
     * Validated at the service layer — not constrained at DB level to allow
     * older data and easy schema evolution.
     */
    @Column
    private Integer rating;

    // ── Moderation ────────────────────────────────────────────────────────────

    /**
     * Admin approval gate.
     * {@code false}  → comment is pending review, not shown publicly.
     * {@code true}   → comment has been approved and is visible on the portfolio.
     */
    @Column(nullable = false)
    private Boolean approved = false;

    /**
     * Optional admin note about why a comment was rejected / held.
     * Visible only in the admin portal, never sent to the client.
     */
    @Column(length = 500)
    private String adminNote;

    // ── Audit timestamps ──────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public PreviousProjectComment() {}

    /**
     * Convenience constructor for the most common creation path.
     *
     * @param previousProject the project being commented on
     * @param client          the logged-in client (may be null for anonymous legacy data)
     * @param authorDisplayName denormalised name captured at post time
     * @param content         comment body text
     * @param rating          optional 1–5 star rating (pass null to omit)
     */
    public PreviousProjectComment(PreviousProject previousProject,
                                   Client client,
                                   String authorDisplayName,
                                   String content,
                                   Integer rating) {
        this.previousProject   = previousProject;
        this.client            = client;
        this.authorDisplayName = authorDisplayName;
        this.content           = content;
        this.rating            = rating;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                                   { return id; }

    public PreviousProject getPreviousProject()           { return previousProject; }
    public void setPreviousProject(PreviousProject p)     { this.previousProject = p; }

    public Client getClient()                             { return client; }
    public void setClient(Client client)                  { this.client = client; }

    public String getAuthorDisplayName()                  { return authorDisplayName; }
    public void setAuthorDisplayName(String name)         { this.authorDisplayName = name; }

    public String getContent()                            { return content; }
    public void setContent(String content)                { this.content = content; }

    public Integer getRating()                            { return rating; }
    public void setRating(Integer rating)                 { this.rating = rating; }

    public Boolean getApproved()                          { return approved; }
    public void setApproved(Boolean approved)             { this.approved = approved; }

    public String getAdminNote()                          { return adminNote; }
    public void setAdminNote(String adminNote)            { this.adminNote = adminNote; }

    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public LocalDateTime getUpdatedAt()                   { return updatedAt; }

    @Override
    public String toString() {
        return "PreviousProjectComment{id=" + id +
               ", projectId=" + (previousProject != null ? previousProject.getId() : "null") +
               ", author='" + authorDisplayName + "'" +
               ", approved=" + approved + "}";
    }
}
