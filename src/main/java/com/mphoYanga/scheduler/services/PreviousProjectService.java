package com.mphoYanga.scheduler.services;

import com.mphoYanga.scheduler.models.Client;
import com.mphoYanga.scheduler.models.PreviousProject;
import com.mphoYanga.scheduler.models.PreviousProject.ServiceCategory;
import com.mphoYanga.scheduler.models.PreviousProjectComment;
import com.mphoYanga.scheduler.models.PreviousProjectImage;
import com.mphoYanga.scheduler.repos.ClientRepository;
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

    @Value("${project.upload.dir}")
    private String uploadDir;

    private static final String URL_PREFIX = "/uploads/previous-projects";

    private final PreviousProjectRepository        projectRepo;
    private final PreviousProjectImageRepository   imageRepo;
    private final PreviousProjectCommentRepository commentRepo;
    private final ClientRepository                 clientRepo;

    public PreviousProjectService(PreviousProjectRepository projectRepo,
                                  PreviousProjectImageRepository imageRepo,
                                  PreviousProjectCommentRepository commentRepo,
                                  ClientRepository clientRepo) {
        this.projectRepo  = projectRepo;
        this.imageRepo    = imageRepo;
        this.commentRepo  = commentRepo;
        this.clientRepo   = clientRepo;
    }

    // ── PROJECTS ──────────────────────────────────────────────────────────────

    public List<PreviousProject> getAllForAdmin() {
        return projectRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<PreviousProject> getPublishedWithImages() {
        return projectRepo.findPublishedWithImages();
    }

    public Optional<PreviousProject> getById(Long id) {
        return projectRepo.findById(id);
    }

    @Transactional
    public PreviousProject create(String name, String description, String location,
                                  ServiceCategory category, Integer completionYear,
                                  boolean published, MultipartFile[] imageFiles) throws IOException {
        PreviousProject project = new PreviousProject(name, description, location, category, completionYear);
        project.setPublished(published);
        project = projectRepo.save(project);

        if (imageFiles != null) {
            boolean firstImage = true;
            for (MultipartFile file : imageFiles) {
                if (file == null || file.isEmpty()) continue;
                PreviousProjectImage img = saveImageFile(project, file, firstImage);
                project.addImage(img);
                firstImage = false;
            }
            project = projectRepo.save(project);
        }
        log.info("Created PreviousProject id={} name='{}' images={}", project.getId(), name, project.getImages().size());
        return project;
    }

    @Transactional
    public PreviousProject update(Long id, Map<String, Object> fields) {
        PreviousProject p = projectRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
        if (fields.containsKey("name"))        p.setName((String) fields.get("name"));
        if (fields.containsKey("description")) p.setDescription((String) fields.get("description"));
        if (fields.containsKey("location"))    p.setLocation((String) fields.get("location"));
        if (fields.containsKey("category"))    p.setCategory(ServiceCategory.valueOf((String) fields.get("category")));
        if (fields.containsKey("completionYear")) {
            Object y = fields.get("completionYear");
            p.setCompletionYear(y != null ? ((Number) y).intValue() : null);
        }
        return projectRepo.save(p);
    }

    @Transactional
    public PreviousProject setPublished(Long id, boolean published) {
        PreviousProject p = projectRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
        p.setPublished(published);
        return projectRepo.save(p);
    }

    @Transactional
    public void delete(Long id) {
        PreviousProject p = projectRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
        for (PreviousProjectImage img : p.getImages()) deleteImageFile(img.getFilePath());
        try {
            Path folder = Paths.get(uploadDir, "previous-projects", String.valueOf(id));
            if (Files.exists(folder)) {
                Files.walk(folder).sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile).forEach(java.io.File::delete);
            }
        } catch (IOException e) { log.warn("Could not delete project folder id={}: {}", id, e.getMessage()); }
        projectRepo.delete(p);
        log.info("Deleted PreviousProject id={}", id);
    }

    // ── IMAGES ────────────────────────────────────────────────────────────────

    @Transactional
    public PreviousProject addImages(Long projectId, MultipartFile[] files) throws IOException {
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

    @Transactional
    public void deleteImage(Long projectId, Long imageId) {
        PreviousProjectImage img = imageRepo.findByIdAndPreviousProjectId(imageId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        deleteImageFile(img.getFilePath());
        imageRepo.delete(img);
    }

    @Transactional
    public void setCoverImage(Long projectId, Long imageId) {
        imageRepo.clearCoverImageForProject(projectId);
        PreviousProjectImage img = imageRepo.findByIdAndPreviousProjectId(imageId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        img.setCoverImage(true);
        imageRepo.save(img);
    }

    // ── COMMENTS — ADMIN ──────────────────────────────────────────────────────

    public List<PreviousProjectComment> getCommentsForAdmin(Long projectId) {
        return commentRepo.findByPreviousProjectIdOrderByCreatedAtDesc(projectId);
    }

    public List<PreviousProjectComment> getApprovedComments(Long projectId) {
        return commentRepo.findByPreviousProjectIdAndApprovedTrueOrderByCreatedAtAsc(projectId);
    }

    public List<PreviousProjectComment> getPendingComments() {
        return commentRepo.findByApprovedFalseOrderByCreatedAtAsc();
    }

    @Transactional
    public PreviousProjectComment approveComment(Long projectId, Long commentId) {
        PreviousProjectComment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        if (!c.getPreviousProject().getId().equals(projectId))
            throw new IllegalArgumentException("Comment does not belong to project " + projectId);
        c.setApproved(true);
        return commentRepo.save(c);
    }

    /** Admin can delete any comment. */
    @Transactional
    public void deleteComment(Long projectId, Long commentId) {
        PreviousProjectComment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        if (!c.getPreviousProject().getId().equals(projectId))
            throw new IllegalArgumentException("Comment does not belong to project " + projectId);
        commentRepo.delete(c);
        log.info("Admin deleted comment id={} from project id={}", commentId, projectId);
    }

    // ── COMMENTS — CLIENT ─────────────────────────────────────────────────────

    /**
     * Posts a comment from a logged-in client.
     * Starts as pending (approved=false) until an admin approves it.
     */
    @Transactional
    public PreviousProjectComment postClientComment(Long projectId, Long clientId,
                                                    String content, Integer rating) {
        if (content == null || content.isBlank())
            throw new IllegalArgumentException("Comment content cannot be empty.");
        if (rating != null && (rating < 1 || rating > 5))
            throw new IllegalArgumentException("Rating must be between 1 and 5.");

        PreviousProject project = projectRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        if (!Boolean.TRUE.equals(project.getPublished()))
            throw new IllegalArgumentException("Cannot comment on an unpublished project.");

        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        String displayName = (client.getName() + " " + (client.getSurname() != null ? client.getSurname() : "")).trim();

        PreviousProjectComment comment = new PreviousProjectComment(
                project, client, displayName, content.trim(), rating
        );
        comment.setApproved(false);

        PreviousProjectComment saved = commentRepo.save(comment);
        log.info("Client {} posted comment on project id={} (pending approval)", clientId, projectId);
        return saved;
    }

    /**
     * Allows a client to delete ONLY their own comment.
     * Throws SecurityException if the comment belongs to a different client.
     */
    @Transactional
    public void deleteClientComment(Long projectId, Long commentId, Long clientId) {
        PreviousProjectComment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        if (!c.getPreviousProject().getId().equals(projectId))
            throw new IllegalArgumentException("Comment does not belong to project " + projectId);
        if (c.getClient() == null || !c.getClient().getId().equals(clientId))
            throw new SecurityException("You can only delete your own comments.");
        commentRepo.delete(c);
        log.info("Client {} deleted own comment id={} from project id={}", clientId, commentId, projectId);
    }

    // ── STATS ─────────────────────────────────────────────────────────────────

    public Map<String, Long> getStats() {
        long total     = projectRepo.count();
        long published = projectRepo.countByPublishedTrue();
        long pending   = commentRepo.countByApprovedFalse();
        return Map.of("total", total, "published", published,
                "draft", total - published, "pendingComments", pending);
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private PreviousProjectImage saveImageFile(PreviousProject project,
                                               MultipartFile file, boolean isCover) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) ext = originalName.substring(dot);

        String uniqueName = UUID.randomUUID().toString() + ext;
        Path folder = Paths.get(uploadDir, "previous-projects", String.valueOf(project.getId()));
        Files.createDirectories(folder);
        Files.copy(file.getInputStream(), folder.resolve(uniqueName), StandardCopyOption.REPLACE_EXISTING);

        String relPath = "previous-projects/" + project.getId() + "/" + uniqueName;
        String imgUrl  = URL_PREFIX + "/" + project.getId() + "/" + uniqueName;

        PreviousProjectImage img = new PreviousProjectImage(
                project, relPath, imgUrl, originalName, file.getContentType(), file.getSize());
        img.setCoverImage(isCover);
        img.setDisplayOrder(project.getImages().size());
        return img;
    }

    private void deleteImageFile(String relPath) {
        if (relPath == null) return;
        try {
            Files.deleteIfExists(Paths.get(uploadDir, relPath.replace('/', java.io.File.separatorChar)));
        } catch (IOException e) { log.warn("Could not delete image file '{}': {}", relPath, e.getMessage()); }
    }
}