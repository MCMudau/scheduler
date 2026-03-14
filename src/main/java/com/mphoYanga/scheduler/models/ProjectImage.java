package com.mphoYanga.scheduler.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_images")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProjectImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    @JsonIgnore
    private ProjectStage stage;

    @Column(nullable = false, length = 255)
    private String fileName; // Original filename

    @Column(nullable = false, columnDefinition = "TEXT")
    private String filePath; // Relative path: /projects/{projectId}/stage{stageNumber}/{fileName}

    @Column(columnDefinition = "TEXT")
    private String caption; // Optional caption or comment about the image

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false)
    private Long fileSize; // Size in bytes

    @Column(nullable = false, length = 50)
    private String contentType; // e.g., "image/jpeg", "image/png"

    // ── Constructors ──────────────────────────────────────────

    public ProjectImage() {}

    public ProjectImage(ProjectStage stage, String fileName, String filePath,
                        Long fileSize, String contentType) {
        this.stage = stage;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public ProjectStage getStage() { return stage; }
    public void setStage(ProjectStage stage) { this.stage = stage; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    @Override
    public String toString() {
        return "ProjectImage{id=" + id + ", fileName='" + fileName + 
               "', stage=" + (stage != null ? stage.getId() : "N/A") + "}";
    }
}
