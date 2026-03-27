package com.mphoYanga.scheduler.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A single image file attached to a {@link PreviousProject}.
 *
 * Ownership rule: one PreviousProjectImage belongs to exactly one PreviousProject.
 * The foreign key column {@code previous_project_id} lives on this table.
 *
 * File storage strategy (consistent with the rest of the application):
 *   - The raw file is saved to disk under {@code project.upload.dir}
 *     (or a sub-folder of it, e.g. {@code previous-projects/}).
 *   - {@code filePath} stores the relative path with forward slashes so it is
 *     OS-independent — e.g. {@code "previous-projects/42/abc123.jpg"}.
 *   - {@code imageUrl} is the public-facing URL served by the {@code /uploads/}
 *     static resource mapping — e.g. {@code "/uploads/previous-projects/42/abc123.jpg"}.
 */
@Entity
@Table(name = "previous_project_images")
public class PreviousProjectImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Parent relationship ───────────────────────────────────────────────────

    /**
     * The project this image belongs to.
     * {@code @JsonBackReference} suppresses this side during serialisation
     * to break the circular loop with {@code PreviousProject.images}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "previous_project_id", nullable = false)
    @JsonBackReference("project-images")
    private PreviousProject previousProject;

    // ── File metadata ─────────────────────────────────────────────────────────

    /**
     * Relative file path on disk, forward-slash separated.
     * Example: {@code "previous-projects/42/20240312_abc.jpg"}
     * Never store an OS-absolute path — store relative to the upload root.
     */
    @Column(nullable = false, length = 500)
    private String filePath;

    /**
     * Public URL served by the static resource handler.
     * Example: {@code "/uploads/previous-projects/42/20240312_abc.jpg"}
     */
    @Column(nullable = false, length = 500)
    private String imageUrl;

    /** Original filename as uploaded by the admin, for display / download hints. */
    @Column(length = 255)
    private String originalFileName;

    /** MIME type of the uploaded file, e.g. {@code "image/jpeg"}, {@code "image/png"}. */
    @Column(length = 50)
    private String mimeType;

    /** File size in bytes — useful for admin UI and storage audits. */
    @Column
    private Long fileSizeBytes;

    // ── Display metadata ──────────────────────────────────────────────────────

    /**
     * Optional admin-provided caption shown beneath the image in the gallery.
     * Left null/blank if the admin doesn't supply one.
     */
    @Column(length = 500)
    private String caption;

    /**
     * Zero-based display order within the project gallery.
     * The admin can reorder images; the lowest value is shown first.
     * Defaults to 0 so the first upload appears at the front.
     */
    @Column(nullable = false)
    private Integer displayOrder = 0;

    /**
     * Whether this image is the "cover" / hero image shown on the portfolio
     * listing cards. Only one image per project should have this set to true
     * (enforced at the service layer, not at DB level for simplicity).
     */
    @Column(nullable = false)
    private Boolean coverImage = false;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public PreviousProjectImage() {}

    public PreviousProjectImage(PreviousProject project,
                                 String filePath,
                                 String imageUrl,
                                 String originalFileName,
                                 String mimeType,
                                 Long fileSizeBytes) {
        this.previousProject  = project;
        this.filePath         = filePath;
        this.imageUrl         = imageUrl;
        this.originalFileName = originalFileName;
        this.mimeType         = mimeType;
        this.fileSizeBytes    = fileSizeBytes;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                             { return id; }

    public PreviousProject getPreviousProject()     { return previousProject; }
    public void setPreviousProject(PreviousProject p) { this.previousProject = p; }

    public String getFilePath()                     { return filePath; }
    public void setFilePath(String filePath)        { this.filePath = filePath; }

    public String getImageUrl()                     { return imageUrl; }
    public void setImageUrl(String imageUrl)        { this.imageUrl = imageUrl; }

    public String getOriginalFileName()             { return originalFileName; }
    public void setOriginalFileName(String n)       { this.originalFileName = n; }

    public String getMimeType()                     { return mimeType; }
    public void setMimeType(String mimeType)        { this.mimeType = mimeType; }

    public Long getFileSizeBytes()                  { return fileSizeBytes; }
    public void setFileSizeBytes(Long size)         { this.fileSizeBytes = size; }

    public String getCaption()                      { return caption; }
    public void setCaption(String caption)          { this.caption = caption; }

    public Integer getDisplayOrder()                { return displayOrder; }
    public void setDisplayOrder(Integer order)      { this.displayOrder = order; }

    public Boolean getCoverImage()                  { return coverImage; }
    public void setCoverImage(Boolean coverImage)   { this.coverImage = coverImage; }

    public LocalDateTime getUploadedAt()            { return uploadedAt; }

    @Override
    public String toString() {
        return "PreviousProjectImage{id=" + id +
               ", projectId=" + (previousProject != null ? previousProject.getId() : "null") +
               ", originalFileName='" + originalFileName + "'}";
    }
}
