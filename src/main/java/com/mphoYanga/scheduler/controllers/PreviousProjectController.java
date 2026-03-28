package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.PreviousProject;
import com.mphoYanga.scheduler.models.PreviousProject.ServiceCategory;
import com.mphoYanga.scheduler.models.PreviousProjectComment;
import com.mphoYanga.scheduler.services.PreviousProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the "Past Projects / Portfolio" feature.
 *
 * Base path: /api/previous-projects
 *
 * ── Project endpoints ──────────────────────────────────────────────────────
 *   GET    /api/previous-projects/admin/all          → all projects (admin)
 *   GET    /api/previous-projects/published           → published projects with images
 *   GET    /api/previous-projects/{id}                → single project by id
 *   POST   /api/previous-projects                     → create project (multipart)
 *   PUT    /api/previous-projects/{id}/publish        → flip published flag
 *   DELETE /api/previous-projects/{id}                → delete project + images + comments
 *
 * ── Image endpoints ────────────────────────────────────────────────────────
 *   POST   /api/previous-projects/{id}/images         → add images to existing project
 *   DELETE /api/previous-projects/{id}/images/{imgId} → delete one image
 *   PUT    /api/previous-projects/{id}/images/{imgId}/cover → set as cover image
 *
 * ── Comment endpoints ──────────────────────────────────────────────────────
 *   GET    /api/previous-projects/{id}/comments/admin     → all comments (admin)
 *   GET    /api/previous-projects/{id}/comments           → approved comments (clients)
 *   PUT    /api/previous-projects/{id}/comments/{cId}/approve → approve a comment
 *   DELETE /api/previous-projects/{id}/comments/{cId}     → delete a comment
 *
 * ── Stats ───────────────────────────────────────────────────────────────────
 *   GET    /api/previous-projects/stats                   → counts for admin dashboard
 */
@RestController
@RequestMapping("/api/previous-projects")
public class PreviousProjectController {

    private final PreviousProjectService service;

    public PreviousProjectController(PreviousProjectService service) {
        this.service = service;
    }

    // ── PROJECT CRUD ──────────────────────────────────────────────────────────

    /** Admin: all projects regardless of published state. */
    @GetMapping("/admin/all")
    public ResponseEntity<Map<String, Object>> getAllAdmin() {
        try {
            List<PreviousProject> list = service.getAllForAdmin();
            return ok(list);
        } catch (Exception e) {
            return error("Failed to load projects: " + e.getMessage());
        }
    }

    /** Public/Client: published projects with images pre-fetched. */
    @GetMapping("/published")
    public ResponseEntity<Map<String, Object>> getPublished() {
        try {
            List<PreviousProject> list = service.getPublishedWithImages();
            return ok(list);
        } catch (Exception e) {
            return error("Failed to load portfolio: " + e.getMessage());
        }
    }

    /** Single project by ID (admin or public — service level controls visibility if needed). */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return service.getById(id)
                .<ResponseEntity<Map<String, Object>>>map(p -> ok(p))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Project not found")));
    }

    /**
     * Create a new past project.
     * Accepts multipart/form-data so images can be uploaded in the same request.
     *
     * Form fields:
     *   name, description, location, category, completionYear (optional),
     *   published (boolean, default false), images[] (files, optional)
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> create(
            @RequestParam("name")                     String name,
            @RequestParam("description")              String description,
            @RequestParam("location")                 String location,
            @RequestParam("category")                 String category,
            @RequestParam(value = "completionYear",   required = false) Integer completionYear,
            @RequestParam(value = "published",        defaultValue = "false") boolean published,
            @RequestParam(value = "images",           required = false) MultipartFile[] images) {
        try {
            ServiceCategory cat = ServiceCategory.valueOf(category.toUpperCase());
            PreviousProject project = service.create(name, description, location, cat,
                    completionYear, published, images);
            return ResponseEntity.status(201).body(Map.of(
                    "success", true,
                    "message", "Project created successfully.",
                    "data",    project
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid category: " + category));
        } catch (Exception e) {
            return error("Failed to create project: " + e.getMessage());
        }
    }

    /** Flip the published flag. Body: { "published": true|false } */
    @PutMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> setPublished(
            @PathVariable Long id,
            @RequestBody  Map<String, Boolean> body) {
        try {
            Boolean published = body.get("published");
            if (published == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "'published' field is required."));
            }
            PreviousProject updated = service.setPublished(id, published);
            return ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return error("Failed to update project: " + e.getMessage());
        }
    }

    /** Delete a project and all associated images + comments. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Project deleted."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return error("Failed to delete project: " + e.getMessage());
        }
    }

    // ── IMAGES ────────────────────────────────────────────────────────────────

    /** Add more images to an existing project. */
    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> addImages(
            @PathVariable Long id,
            @RequestParam("images") MultipartFile[] files) {
        try {
            PreviousProject updated = service.addImages(id, files);
            return ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return error("Failed to upload images: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
// ADD THESE TWO METHODS to PreviousProjectController.java
// They sit alongside the existing comment endpoints.
// ─────────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/previous-projects/{id}/comments/client
     * A logged-in client posts a new comment on a published project.
     *
     * Body: { "clientId": 42, "content": "Great work!", "rating": 5 }
     *
     * Comment starts as pending (approved=false).
     * Admin must approve it before it appears on the public page.
     */
    @PostMapping("/{id}/comments/client")
    public ResponseEntity<Map<String, Object>> postClientComment(
            @PathVariable Long id,
            @RequestBody  Map<String, Object> body) {
        try {
            Object rawClientId = body.get("clientId");
            if (rawClientId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "clientId is required."));
            }
            Long   clientId = ((Number) rawClientId).longValue();
            String content  = (String) body.get("content");
            Integer rating  = body.get("rating") != null ? ((Number) body.get("rating")).intValue() : null;

            PreviousProjectComment saved = service.postClientComment(id, clientId, content, rating);
            return ResponseEntity.status(201).body(Map.of(
                    "success", true,
                    "message", "Comment submitted and awaiting approval.",
                    "data",    saved
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return error("Failed to post comment: " + e.getMessage());
        }
    }

    /**
     * DELETE /api/previous-projects/{id}/comments/{commentId}/client
     * A client deletes their own comment. Ownership is enforced in the service.
     *
     * Body: { "clientId": 42 }
     */
    @DeleteMapping("/{id}/comments/{commentId}/client")
    public ResponseEntity<Map<String, Object>> deleteClientComment(
            @PathVariable Long id,
            @PathVariable Long commentId,
            @RequestBody  Map<String, Object> body) {
        try {
            Object rawClientId = body.get("clientId");
            if (rawClientId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "clientId is required."));
            }
            Long clientId = ((Number) rawClientId).longValue();
            service.deleteClientComment(id, commentId, clientId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Comment deleted."));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return error("Failed to delete comment: " + e.getMessage());
        }
    }

    /** Delete a single image from a project. */
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Map<String, Object>> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        try {
            service.deleteImage(id, imageId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Image deleted."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return error("Failed to delete image: " + e.getMessage());
        }
    }

    /** Mark an image as the cover image for a project. */
    @PutMapping("/{id}/images/{imageId}/cover")
    public ResponseEntity<Map<String, Object>> setCover(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        try {
            service.setCoverImage(id, imageId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cover image updated."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return error("Failed to set cover image: " + e.getMessage());
        }
    }

    // ── COMMENTS ──────────────────────────────────────────────────────────────

    /** Admin: all comments for a project (pending + approved). */
    @GetMapping("/{id}/comments/admin")
    public ResponseEntity<Map<String, Object>> getCommentsAdmin(@PathVariable Long id) {
        try {
            List<PreviousProjectComment> comments = service.getCommentsForAdmin(id);
            return ok(comments);
        } catch (Exception e) {
            return error("Failed to load comments: " + e.getMessage());
        }
    }

    /** Public/Client: approved comments only. */
    @GetMapping("/{id}/comments")
    public ResponseEntity<Map<String, Object>> getApprovedComments(@PathVariable Long id) {
        try {
            List<PreviousProjectComment> comments = service.getApprovedComments(id);
            return ok(comments);
        } catch (Exception e) {
            return error("Failed to load comments: " + e.getMessage());
        }
    }

    /** Approve a comment — makes it publicly visible. */
    @PutMapping("/{id}/comments/{commentId}/approve")
    public ResponseEntity<Map<String, Object>> approveComment(
            @PathVariable Long id,
            @PathVariable Long commentId) {
        try {
            PreviousProjectComment updated = service.approveComment(id, commentId);
            return ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return error("Failed to approve comment: " + e.getMessage());
        }
    }

    /** Delete a comment. */
    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long id,
            @PathVariable Long commentId) {
        try {
            service.deleteComment(id, commentId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Comment deleted."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return error("Failed to delete comment: " + e.getMessage());
        }
    }

    // ── STATS ─────────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Long> stats = service.getStats();
            return ResponseEntity.ok(Map.of("success", true, "data", stats));
        } catch (Exception e) {
            return error("Failed to load stats: " + e.getMessage());
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    private ResponseEntity<Map<String, Object>> error(String message) {
        return ResponseEntity.status(500).body(Map.of("success", false, "message", message));
    }
}
