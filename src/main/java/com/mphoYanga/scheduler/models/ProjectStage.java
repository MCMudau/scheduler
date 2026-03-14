package com.mphoYanga.scheduler.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "project_stages")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProjectStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;

    @Column(nullable = false, length = 150)
    private String stageName; // e.g., "Site Preparation", "Plumbing Installation", "Tiling", etc.

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer stageNumber; // Order of the stage (1, 2, 3, etc.)

    @Column(columnDefinition = "TEXT")
    private String comments; // Comments about this stage

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin capturedBy; // The admin who created this update

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProjectImage> images;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updateTime;

    @Column
    private Integer imageCount = 0;

    // ── Constructors ──────────────────────────────────────────

    public ProjectStage() {}

    public ProjectStage(Project project, String stageName, String description,
                        Integer stageNumber, Admin capturedBy) {
        this.project = project;
        this.stageName = stageName;
        this.description = description;
        this.stageNumber = stageNumber;
        this.capturedBy = capturedBy;
        this.updateTime = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getStageNumber() { return stageNumber; }
    public void setStageNumber(Integer stageNumber) { this.stageNumber = stageNumber; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Admin getCapturedBy() { return capturedBy; }
    public void setCapturedBy(Admin capturedBy) { this.capturedBy = capturedBy; }

    public List<ProjectImage> getImages() { return images; }
    public void setImages(List<ProjectImage> images) { this.images = images; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getImageCount() { return imageCount; }
    public void setImageCount(Integer imageCount) { this.imageCount = imageCount; }

    @Override
    public String toString() {
        return "ProjectStage{id=" + id + ", stageName='" + stageName + 
               "', stageNumber=" + stageNumber + ", project=" + 
               (project != null ? project.getName() : "N/A") + "}";
    }
}
