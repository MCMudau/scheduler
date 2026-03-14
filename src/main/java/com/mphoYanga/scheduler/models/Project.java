package com.mphoYanga.scheduler.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "projects")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.ONGOING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String serviceType; // e.g., "Plumbing", "Tiling", "Painting", "Ceiling Installation"

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProjectStage> stages;

    @Column(nullable = false)
    private Integer stageCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String projectDirectory; // Path to store project images: /projects/{projectId}/

    @Column(nullable = false)
    private Boolean completed = false;

    @Column
    private LocalDateTime completedAt;

    @Column
    private Double estimatedBudget;

    @Column
    private Double actualBudget;

    // ── Constructors ──────────────────────────────────────────

    public Project() {}

    public Project(String name, String description, Admin createdBy, 
                   Client client, String location, String serviceType) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.client = client;
        this.location = location;
        this.serviceType = serviceType;
        this.status = ProjectStatus.ONGOING;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public Admin getCreatedBy() { return createdBy; }
    public void setCreatedBy(Admin createdBy) { this.createdBy = createdBy; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public List<ProjectStage> getStages() { return stages; }
    public void setStages(List<ProjectStage> stages) { this.stages = stages; }

    public Integer getStageCount() { return stageCount; }
    public void setStageCount(Integer stageCount) { this.stageCount = stageCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getProjectDirectory() { return projectDirectory; }
    public void setProjectDirectory(String projectDirectory) { this.projectDirectory = projectDirectory; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Double getEstimatedBudget() { return estimatedBudget; }
    public void setEstimatedBudget(Double estimatedBudget) { this.estimatedBudget = estimatedBudget; }

    public Double getActualBudget() { return actualBudget; }
    public void setActualBudget(Double actualBudget) { this.actualBudget = actualBudget; }

    @Override
    public String toString() {
        return "Project{id=" + id + ", name='" + name + "', status=" + status + 
               ", client='" + (client != null ? client.getEmail() : "N/A") + "'}";
    }

    // ── Enum for Project Status ───────────────────────────────

    public enum ProjectStatus {
        ONGOING("Ongoing"),
        PAUSED("Paused"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");

        private final String displayName;

        ProjectStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
