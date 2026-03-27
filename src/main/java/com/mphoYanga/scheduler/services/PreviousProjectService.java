package com.mphoYanga.scheduler.services;

import com.mphoYanga.scheduler.models.PreviousProject;
import com.mphoYanga.scheduler.models.PreviousProject.ServiceCategory;
import com.mphoYanga.scheduler.models.PreviousProjectComment;
import com.mphoYanga.scheduler.models.PreviousProjectImage;
import com.mphoYanga.scheduler.repos.PreviousProjectCommentRepository;
import com.mphoYanga.scheduler.repos.PreviousProjectImageRepository;
import com.mphoYanga.scheduler.repos.PreviousProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PreviousProjectService {

    private static final Logger log = LoggerFactory.getLogger(PreviousProjectService.class);

    // Upload directory root from application.properties, e.g. /var/mpho/uploads
    // Images are stored under: <upload-dir>/previous-projects/<projectId>/<uuid>.<ext>
    @Value("${project.upload.dir}")
    private String uploadDir;

    // Static URL prefix served by Spring's resource handler, e.g. /uploads
    private static final String URL_PREFIX = "/uploads/previous-projects";

    private final PreviousProjectRepository        projectRepo;
    private final PreviousProjectImageRepository   imageRepo;
    private final PreviousProjectCommentRepository commentRepo;

    public PreviousProjectService(PreviousProjectRepository projectRepo,
                                   PreviousProjectImageRepository imageRepo,
                                   PreviousProjectCommentRepository commentRepo) {
        this.projectRepo  = projectRepo;
        this.imageRepo    = imageRepo;
        this.commentRepo  = commentRepo;
    }

    // ── PROJECTS ──────────────────────────────────────────────────────────────

    /** All projects for the admin panel (published + drafts), newest first. */
    public List<PreviousProject> getAllForAdmin() {
        return projectRepo.findAllByOrderByCreatedAtDesc();
    }

    /** Published projects only, with images pre-fetched (for client portfolio). */
    public List<PreviousProject> getPublishedWithImages() {
        return projectRepo.findPublishedWithImages();
    }

    /** Single project by ID. */
    public Optional<PreviousProject> getById(Long id) {
        return projectRepo.findById(id);
    }

    /**
     * Creates a new PreviousProject, saves it, then uploads any images.
     * Uses a two-phase save so the project ID is available for the storage path.
     */
    @Transactional
    public PreviousProject create(String name,
                                   String description,
                                   String location,
                                   ServiceCategory category,
                                   Integer completionYear,
                                   boolean published,
                                   MultipartFile[] imageFiles) throws IOException {

        // Phase 1: save entity to get the generated ID
        PreviousProject project = new PreviousProject(name, description, location, category, completionYear);
        project.setPublished(published);
        project = projectRepo.save(project);

        // Phase 2: save images (if any)
        if (imageFiles != null) {
            boolean firstImage = true;
            for (MultipartFile file : imageFiles) {
                if (file == null || file.isEmpty()) continue;
                PreviousProjectImage img = saveImageFile(project, file, firstImage);
                project.addImage(img);
                firstImage = false;
            }
            // Persist images via cascade
            project = projectRepo.save(project);
        }

        log.info("Created PreviousProject id={} name='{}' images={}", project.getId(), name,
                project.getImages().size());
        return project;
    }

    /** Flip the published flag on a project. */
    @Transactional
    public PreviousProject setPublished(Long id, boolean published) {
        PreviousProject p = projectRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
        p.setPublished(published);
        return projectRepo.save(p);
    }

    /**
     * Deletes a project and all its images from disk and DB.
     * Cascade ALL + orphanRemoval handles DB child rows automatically.
     */
    @Transactional
    public void delete(Long id) {
        PreviousProject p = projectRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));

        // Delete image files from disk
        for (PreviousProjectImage img : p.getImages()) {
            deleteImageFile(img.getFilePath());
        }

        // Try to remove the project folder too (best-effort)
        try {
            Path folder = Paths.get(uploadDir, "previous-projects", String.valueOf(id));
            if (Files.exists(folder)) {
                Files.walk(folder)
                     .sorted(java.util.Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(java.io.File::delete);
            }
        } catch (IOException e) {
            log.warn("Could not delete project folder for id={}: {}", id, e.getMessage());
        }

        projectRepo.delete(p);
        log.info("Deleted PreviousProject id={}", id);
    }

    // ── IMAGES ────────────────────────────────────────────────────────────────

    /**
     * Adds new images to an existing project.
     * New images are appended; existing images are not disturbed.
     */
    @Transactional
    public PreviousProject addImages(Long projectId, MultipartFile[] files) throws IOException {
        // Re-fetch a fresh managed entity (critical — orphanRemoval active)
        PreviousProject project = projectRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        boolean isFirst = project.getImages().isEmpty();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            PreviousProjectImage img = saveImageFile(project, file, isFirst);
            project.addImage(img);
            isFirst = false;
        }
        return projectRepo.save(project);
    }

    /** Deletes a single image from a project. */
    @Transactional
    public void deleteImage(Long projectId, Long imageId) {
        PreviousProjectImage img = imageRepo.findByIdAndPreviousProjectId(imageId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        deleteImageFile(img.getFilePath());
        imageRepo.delete(img);
    }

    /** Marks one image as the cover (clears the flag on all others first). */
    @Transactional
    public void setCoverImage(Long projectId, Long imageId) {
        imageRepo.clearCoverImageForProject(projectId);
        PreviousProjectImage img = imageRepo.findByIdAndPreviousProjectId(imageId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        img.setCoverImage(true);
        imageRepo.save(img);
    }

    // ── COMMENTS ──────────────────────────────────────────────────────────────

    /** All comments for a project (admin view — all regardless of approval). */
    public List<PreviousProjectComment> getCommentsForAdmin(Long projectId) {
        return commentRepo.findByPreviousProjectIdOrderByCreatedAtDesc(projectId);
    }

    /** Approved comments for a project (client-facing view). */
    public List<PreviousProjectComment> getApprovedComments(Long projectId) {
        return commentRepo.findByPreviousProjectIdAndApprovedTrueOrderByCreatedAtAsc(projectId);
    }

    /** Pending (unapproved) comments across all projects. */
    public List<PreviousProjectComment> getPendingComments() {
        return commentRepo.findByApprovedFalseOrderByCreatedAtAsc();
    }

    /** Approves a comment so it becomes publicly visible. */
    @Transactional
    public PreviousProjectComment approveComment(Long projectId, Long commentId) {
        PreviousProjectComment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        if (!c.getPreviousProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Comment does not belong to project " + projectId);
        }
        c.setApproved(true);
        return commentRepo.save(c);
    }

    /** Deletes a comment (admin can delete any comment). */
    @Transactional
    public void deleteComment(Long projectId, Long commentId) {
        PreviousProjectComment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        if (!c.getPreviousProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Comment does not belong to project " + projectId);
        }
        commentRepo.delete(c);
        log.info("Deleted comment id={} from project id={}", commentId, projectId);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    /**
     * Saves a MultipartFile to disk under:
     *   <uploadDir>/previous-projects/<projectId>/<uuid>.<extension>
     *
     * Uses NIO Files.copy (consistent with ImageService pattern in the rest of
     * the application — avoids transferTo() Tomcat temp-dir issues).
     */
    private PreviousProjectImage saveImageFile(PreviousProject project,
                                                MultipartFile file,
                                                boolean isCover) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) ext = originalName.substring(dot); // includes the dot, e.g. ".jpg"

        String uniqueName = UUID.randomUUID().toString() + ext;

        // Build the OS path — use the project ID as a subfolder
        Path folder = Paths.get(uploadDir, "previous-projects", String.valueOf(project.getId()));
        Files.createDirectories(folder);
        Path dest = folder.resolve(uniqueName);

        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        // Build the relative forward-slash path stored in DB
        String relPath = "previous-projects/" + project.getId() + "/" + uniqueName;
        String imgUrl  = URL_PREFIX + "/" + project.getId() + "/" + uniqueName;

        PreviousProjectImage img = new PreviousProjectImage(
                project,
                relPath,
                imgUrl,
                originalName,
                file.getContentType(),
                file.getSize()
        );
        img.setCoverImage(isCover);
        img.setDisplayOrder(project.getImages().size()); // append order

        return img;
    }

    /** Deletes an image file from disk. Logs a warning if the file is missing. */
    private void deleteImageFile(String relPath) {
        if (relPath == null) return;
        try {
            Path p = Paths.get(uploadDir, relPath.replace('/', java.io.File.separatorChar));
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("Could not delete image file '{}': {}", relPath, e.getMessage());
        }
    }

    // ── STATS ─────────────────────────────────────────────────────────────────

    public Map<String, Long> getStats() {
        long total     = projectRepo.count();
        long published = projectRepo.countByPublishedTrue();
        long pending   = commentRepo.countByApprovedFalse();
        return Map.of(
                "total",    total,
                "published", published,
                "draft",    total - published,
                "pendingComments", pending
        );
    }
}
