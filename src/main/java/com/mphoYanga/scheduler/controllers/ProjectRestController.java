package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.*;
import com.mphoYanga.scheduler.services.ProjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ProjectRestController {

    @Autowired
    private ProjectService projectService;

    // ── PROJECT ENDPOINTS ──────────────────── ─────────────────

    /**
     * Create a new project
     * POST /api/projects/create
     */
    @PostMapping("/create")
    public ResponseEntity<?> createProject(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Long adminId,
            @RequestParam Long clientId,
            @RequestParam String location,
            @RequestParam String serviceType,
            @RequestParam(required = false) Double estimatedBudget) {
        try {
            Project project = projectService.createProject(name, description, adminId, clientId,
                    location, serviceType, estimatedBudget);
            return ResponseEntity.ok(new ApiResponse(true, "Project created successfully", project));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Get project by ID
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable Long id) {
        try {
            Optional<Project> project = projectService.getProjectById(id);
            if (project.isPresent()) {
                return ResponseEntity.ok(new ApiResponse(true, "Project retrieved", project.get()));
            } else {
                return ResponseEntity.status(404).body(new ApiResponse(false, "Project not found", null));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Get all projects for an admin
     * GET /api/projects/admin/{adminId}
     */
    @GetMapping("/admin/{adminId}")
    public ResponseEntity<?> getAdminProjects(@PathVariable Long adminId) {
        try {
            List<Project> projects = projectService.getAdminProjects(adminId);
            return ResponseEntity.ok(new ApiResponse(true, "Projects retrieved", projects));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Get all projects for a client
     * GET /api/projects/client/{clientId}
     * Accessible to both clients and admins
     */

    /**
     * Get all images for a project across all stages
     * GET /api/projects/{projectId}/images
     */
    @GetMapping("/{projectId}/images")
    public ResponseEntity<?> getAllImagesForProject(@PathVariable Long projectId) {
        try {
            List<ProjectImage> images = projectService.getProjectImages(projectId);
            return ResponseEntity.ok(new ApiResponse(true, "All project images retrieved", images));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "Failed to retrieve project images: " + e.getMessage(), null));
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getClientProjects(@PathVariable Long clientId) {
        try {
            List<Project> projects = projectService.getClientProjects(clientId);
            return ResponseEntity.ok(new ApiResponse(true, "Client projects retrieved", projects));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Get all active projects
     * GET /api/projects/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveProjects() {
        try {
            List<Project> projects = projectService.getActiveProjects();
            return ResponseEntity.ok(new ApiResponse(true, "Active projects retrieved", projects));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Update project status
     * PUT /api/projects/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateProjectStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            Project.ProjectStatus projectStatus = Project.ProjectStatus.valueOf(status.toUpperCase());
            Project project = projectService.updateProjectStatus(id, projectStatus);
            return ResponseEntity.ok(new ApiResponse(true, "Project status updated", project));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Delete a project
     * DELETE /api/projects/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        try {
            projectService.deleteProject(id);
            return ResponseEntity.ok(new ApiResponse(true, "Project deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ── STAGE ENDPOINTS ───────────────────────────────────────

    /**
     * Create a new stage for a project
     * POST /api/projects/{projectId}/stages
     */
    @PostMapping("/{projectId}/stages")
    public ResponseEntity<?> createStage(
            @PathVariable Long projectId,
            @RequestParam String stageName,
            @RequestParam String description,
            @RequestParam(required = false) String comments, HttpSession session) {
        try {
            // Get adminId from session if not provided
            Long adminId = (Long) session.getAttribute("adminId");
            if (adminId == null) {
                return ResponseEntity.status(401).body(new ApiResponse(false, "Unauthorized: No admin session found", null));
            }
            ProjectStage stage = projectService.createStage(projectId, stageName, description, comments, adminId);
            return ResponseEntity.ok(new ApiResponse(true, "Stage created successfully", stage));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Get all stages for a project
     * GET /api/projects/{projectId}/stages
     */
    @GetMapping("/{projectId}/stages")
    public ResponseEntity<?> getProjectStages(@PathVariable Long projectId) {
        try {
            List<ProjectStage> stages = projectService.getProjectStages(projectId);
            return ResponseEntity.ok(new ApiResponse(true, "Stages retrieved", stages));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Get a specific stage
     * GET /api/projects/stages/{stageId}
     */
    @GetMapping("/stages/{stageId}")
    public ResponseEntity<?> getStage(@PathVariable Long stageId) {
        try {
            Optional<ProjectStage> stage = projectService.getStageById(stageId);
            if (stage.isPresent()) {
                return ResponseEntity.ok(new ApiResponse(true, "Stage retrieved", stage.get()));
            } else {
                return ResponseEntity.status(404).body(new ApiResponse(false, "Stage not found", null));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Update a stage
     * PUT /api/projects/stages/{stageId}
     */
    @PutMapping("/stages/{stageId}")
    public ResponseEntity<?> updateStage(
            @PathVariable Long stageId,
            @RequestParam(required = false) String stageName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String comments) {
        try {
            ProjectStage stage = projectService.updateStage(stageId, stageName, description, comments);
            return ResponseEntity.ok(new ApiResponse(true, "Stage updated", stage));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Delete a stage
     * DELETE /api/projects/{projectId}/stages/{stageId}
     */
    @DeleteMapping("/{projectId}/stages/{stageId}")
    public ResponseEntity<?> deleteStageFromProject(
            @PathVariable Long projectId,
            @PathVariable Long stageId) {
        try {
            projectService.deleteStage(stageId);
            return ResponseEntity.ok(new ApiResponse(true, "Stage deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Delete a stage (legacy path)
     * DELETE /api/projects/stages/{stageId}
     */
    @DeleteMapping("/stages/{stageId}")
    public ResponseEntity<?> deleteStage(@PathVariable Long stageId) {
        try {
            projectService.deleteStage(stageId);
            return ResponseEntity.ok(new ApiResponse(true, "Stage deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Get project statistics for admin
     * GET /api/projects/stats/{adminId}
     */
    @GetMapping("/stats/{adminId}")
    public ResponseEntity<?> getProjectStats(@PathVariable Long adminId) {
        try {
            Map<String, Object> stats = projectService.getProjectStatistics(adminId);
            return ResponseEntity.ok(new ApiResponse(true, "Statistics retrieved", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    /**
     * Get all images for the entire project (all stages)
     * GET /api/projects/{projectId}/stages/{stageId}/all-images
     */
    @GetMapping("/{projectId}/stages/{stageId}/all-images")
    public ResponseEntity<?> getAllProjectImages(
            @PathVariable Long projectId,
            @PathVariable Long stageId) {
        try {
            List<ProjectImage> images = projectService.getProjectImages(projectId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All project images retrieved successfully");
            response.put("imageCount", images.size());
            response.put("images", images);

            return ResponseEntity.ok(new ApiResponse(true, "All project images retrieved", images));

        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "Failed to retrieve project images: " + e.getMessage(), null));
        }
    }

    // ── Helper Classes ────────────────────────────────────────

    @lombok.AllArgsConstructor
    @lombok.Getter
    public static class ApiResponse {
        private Boolean success;
        private String message;
        private Object data;
    }
}