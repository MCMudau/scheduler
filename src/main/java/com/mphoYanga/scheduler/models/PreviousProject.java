package com.mphoYanga.scheduler.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a completed / showcase project that admins upload to build
 * trust with potential new clients.
 *
 * Relationships:
 *   - One PreviousProject  →  Many PreviousProjectImage  (orphanRemoval = true)
 *   - One PreviousProject  →  Many PreviousProjectComment (orphanRemoval = true)
 */
@Entity
@Table(name = "previous_projects")
public class PreviousProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short display title, e.g. "Harare CBD Office Renovation" */
    @Column(nullable = false, length = 200)
    private String name;

    /** Detailed write-up of the work done */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Human-readable location, e.g. "Avondale, Harare" */
    @Column(nullable = false, length = 255)
    private String location;

    /**
     * Service category tag — helps filter the portfolio.
     * Examples: PLUMBING, TILING, PAINTING, CEILING, RENOVATION, OTHER
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ServiceCategory category = ServiceCategory.OTHER;

    /**
     * Optional year the project was completed, e.g. 2024.
     * Stored as a plain integer — no need for a full date if only the year is known.
     */
    @Column
    private Integer completionYear;

    /**
     * Whether this project is publicly visible on the client-facing portfolio page.
     * Admins can draft/hide projects before publishing them.
     */
    @Column(nullable = false)
    private Boolean published = false;

    // ── Images (One-to-Many) ──────────────────────────────────────────────────

    /**
     * All images belonging to this project.
     * cascade = ALL + orphanRemoval means images are deleted when the project
     * is deleted, and images removed from this list are deleted from the DB.
     * IMPORTANT: always use images.clear() + images.addAll() — never setImages()
     * on a managed entity (orphanRemoval constraint).
     */
    @OneToMany(
            mappedBy      = "previousProject",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    @JsonManagedReference("project-images")
    @OrderBy("displayOrder ASC, id ASC")
    private List<PreviousProjectImage> images = new ArrayList<>();

    // ── Comments (One-to-Many) ────────────────────────────────────────────────

    /**
     * All comments left on this project.
     * Comments are also owned by the project — deleting the project removes all
     * its comments automatically.
     */
    @OneToMany(
            mappedBy      = "previousProject",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    @JsonManagedReference("project-comments")
    @OrderBy("createdAt ASC")
    private List<PreviousProjectComment> comments = new ArrayList<>();

    // ── Audit timestamps ──────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public PreviousProject() {}

    public PreviousProject(String name, String description, String location,
                           ServiceCategory category, Integer completionYear) {
        this.name           = name;
        this.description    = description;
        this.location       = location;
        this.category       = category;
        this.completionYear = completionYear;
    }

    // ── Convenience helpers ───────────────────────────────────────────────────

    /** Adds an image and wires up the back-reference. */
    public void addImage(PreviousProjectImage image) {
        image.setPreviousProject(this);
        this.images.add(image);
    }

    /** Removes an image and clears the back-reference. */
    public void removeImage(PreviousProjectImage image) {
        this.images.remove(image);
        image.setPreviousProject(null);
    }

    /** Adds a comment and wires up the back-reference. */
    public void addComment(PreviousProjectComment comment) {
        comment.setPreviousProject(this);
        this.comments.add(comment);
    }

    /** Removes a comment and clears the back-reference. */
    public void removeComment(PreviousProjectComment comment) {
        this.comments.remove(comment);
        comment.setPreviousProject(null);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                          { return id; }

    public String getName()                      { return name; }
    public void setName(String name)             { this.name = name; }

    public String getDescription()               { return description; }
    public void setDescription(String d)         { this.description = d; }

    public String getLocation()                  { return location; }
    public void setLocation(String location)     { this.location = location; }

    public ServiceCategory getCategory()         { return category; }
    public void setCategory(ServiceCategory c)   { this.category = c; }

    public Integer getCompletionYear()           { return completionYear; }
    public void setCompletionYear(Integer y)     { this.completionYear = y; }

    public Boolean getPublished()                { return published; }
    public void setPublished(Boolean published)  { this.published = published; }

    public List<PreviousProjectImage>   getImages()   { return images; }
    public List<PreviousProjectComment> getComments() { return comments; }

    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }

    @Override
    public String toString() {
        return "PreviousProject{id=" + id + ", name='" + name + "', location='" + location +
               "', published=" + published + "}";
    }

    // ── Inner enum ────────────────────────────────────────────────────────────

    /**
     * Service categories matching Mpho Yanga's offering.
     * Stored as a plain String in the DB (EnumType.STRING) so the column
     * remains readable and adding new values never breaks existing rows.
     */
    public enum ServiceCategory {
        PLUMBING,
        TILING,
        PAINTING,
        CEILING,
        RENOVATION,
        OTHER
    }
}
