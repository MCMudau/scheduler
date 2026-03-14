package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.ProjectImage;
import com.mphoYanga.scheduler.services.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/stages/{stageId}/images")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ProjectImageController {

    @Autowired
    private ProjectService projectService;

    /**
     * Upload images for a stage
     * POST /api/projects/{projectId}/stages/{stageId}/images
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadImages(
            @PathVariable Long projectId,
            @PathVariable Long stageId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "captions", required = false) String[] captions) {

        try {
            List<ProjectImage> uploadedImages = projectService.uploadStageImages(stageId, files, captions);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Images uploaded successfully");
            response.put("uploadedCount", uploadedImages.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to upload images: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Get all images for a stage
     * GET /api/projects/{projectId}/stages/{stageId}/images
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStageImages(
            @PathVariable Long projectId,
            @PathVariable Long stageId) {

        try {
            List<ProjectImage> images = projectService.getStageImages(stageId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stage images retrieved successfully");
            response.put("imageCount", images.size());
            response.put("images", images);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve images: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Delete a specific image
     * DELETE /api/projects/{projectId}/stages/{stageId}/images/{imageId}
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Map<String, Object>> deleteImage(
            @PathVariable Long projectId,
            @PathVariable Long stageId,
            @PathVariable Long imageId) {

        try {
            projectService.deleteImage(imageId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete image: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Get all images for the entire project (all stages)
     * GET /api/projects/{projectId}/stages/{stageId}/all-images
     */
    @GetMapping("/all-images")
    public ResponseEntity<Map<String, Object>> getAllProjectImages(@PathVariable Long projectId) {

        try {
            List<ProjectImage> images = projectService.getProjectImages(projectId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All project images retrieved successfully");
            response.put("imageCount", images.size());
            response.put("images", images);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve project images: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Update image caption
     * PUT /api/projects/{projectId}/stages/{stageId}/images/{imageId}
     */
    @PutMapping("/{imageId}")
    public ResponseEntity<Map<String, Object>> updateImageCaption(
            @PathVariable Long projectId,
            @PathVariable Long stageId,
            @PathVariable Long imageId,
            @RequestParam("caption") String caption) {

        try {
            projectService.updateImageCaption(imageId, caption);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image caption updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update image caption: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
