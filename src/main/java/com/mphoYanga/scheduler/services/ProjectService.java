package com.mphoYanga.scheduler.services;

import com.mphoYanga.scheduler.models.*;
import com.mphoYanga.scheduler.repos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectStageRepository projectStageRepository;

    @Autowired
    private ProjectImageRepository projectImageRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Value("${project.upload.dir}")
    private String projectUploadDir;

    @Autowired
    private ImageService imageService;

    // ── PROJECT MANAGEMENT ────────────────────────────────────

    /**
     * Create a new project
     */
    public Project createProject(String name, String description, Long adminId, 
                                Long clientId, String location, String serviceType,
                                Double estimatedBudget) throws Exception {
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new Exception("Admin not found"));
        Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new Exception("Client not found"));

        Project project = new Project(name, description, admin, client, location, serviceType);
        project.setEstimatedBudget(estimatedBudget);

        // Create project directory
        String projectDir = "projects/" + UUID.randomUUID().toString() + "/";
        project.setProjectDirectory(projectDir);

        // Create directories
        createProjectDirectories(projectDir);

        project = projectRepository.save(project);
        return project;
    }

    /**
     * Get project by ID
     */
    public Optional<Project> getProjectById(Long projectId) {
        return projectRepository.findById(projectId);
    }

    /**
     * Get all projects for an admin
     */
    public List<Project> getAdminProjects(Long adminId) throws Exception {
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new Exception("Admin not found"));
        return projectRepository.findByCreatedBy(admin);
    }

    /**
     * Get all projects for a client
     */
    public List<Project> getClientProjects(Long clientId) throws Exception {
        Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new Exception("Client not found"));
        return projectRepository.findByClient(client);
    }

    /**
     * Get all active projects
     */
    public List<Project> getActiveProjects() {
        return projectRepository.findActiveProjects();
    }

    /**
     * Update project status
     */
    public Project updateProjectStatus(Long projectId, Project.ProjectStatus status) throws Exception {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new Exception("Project not found"));
        
        project.setStatus(status);
        
        if (status == Project.ProjectStatus.COMPLETED) {
            project.setCompleted(true);
            project.setCompletedAt(LocalDateTime.now());
        }
        
        return projectRepository.save(project);
    }

    /**
     * Delete a project (and all associated files)
     */
    public void deleteProject(Long projectId) throws Exception {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new Exception("Project not found"));

        // Delete directory
        deleteProjectDirectory(project.getProjectDirectory());

        projectRepository.deleteById(projectId);
    }

    // ── STAGE MANAGEMENT ──────────────────────────────────────

    /**
     * Create a new project stage
     */
    public ProjectStage createStage(Long projectId, String stageName, String description,
                                   String comments, Long adminId) throws Exception {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new Exception("Project not found"));
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new Exception("Admin not found"));

        Integer nextStageNumber = project.getStageCount() + 1;

        ProjectStage stage = new ProjectStage(project, stageName, description, nextStageNumber, admin);
        stage.setComments(comments);

        stage = projectStageRepository.save(stage);

        // Update project stage count
        project.setStageCount(nextStageNumber);
        projectRepository.save(project);

        return stage;
    }

    /**
     * Get all stages for a project
     */
    public List<ProjectStage> getProjectStages(Long projectId) throws Exception {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new Exception("Project not found"));
        return projectStageRepository.findByProjectOrderByStageNumber(project);
    }

    /**
     * Get a specific stage
     */
    public Optional<ProjectStage> getStageById(Long stageId) {
        return projectStageRepository.findById(stageId);
    }

    /**
     * Update a stage
     */
    public ProjectStage updateStage(Long stageId, String stageName, String description,
                                   String comments) throws Exception {
        ProjectStage stage = projectStageRepository.findById(stageId)
            .orElseThrow(() -> new Exception("Stage not found"));

        if (stageName != null) stage.setStageName(stageName);
        if (description != null) stage.setDescription(description);
        if (comments != null) stage.setComments(comments);

        stage.setUpdateTime(LocalDateTime.now());

        return projectStageRepository.save(stage);
    }

    /**
     * Delete a stage and all associated images
     */
    public void deleteStage(Long stageId) throws Exception {
        ProjectStage stage = projectStageRepository.findById(stageId)
            .orElseThrow(() -> new Exception("Stage not found"));

        // Delete all images for this stage
        List<ProjectImage> images = projectImageRepository.findByStageOrderByUploadedAtDesc(stage);
        for (ProjectImage image : images) {
            deleteImageFile(image.getFilePath());
        }

        projectStageRepository.deleteById(stageId);
    }

    // ── IMAGE MANAGEMENT ──────────────────────────────────────

    /**
     * Upload image(s) for a stage
     */



    /**
     * Upload multiple images
     */


    /**
     * Get all images for a stage
     */



    // ── UTILITY METHODS ───────────────────────────────────────

    /**
     * Create project directory structure
     */
    private void createProjectDirectories(String projectDir) throws IOException {
        Path path = Paths.get(projectUploadDir, projectDir);
        Files.createDirectories(path);
    }

    /**
     * Delete entire project directory
     */
    private void deleteProjectDirectory(String projectDir) throws IOException {
        Path path = Paths.get(projectUploadDir, projectDir);
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        }
    }

    /**
     * Delete a single image file
     */
    private void deleteImageFile(String filePath) throws IOException {
        Path path = Paths.get(projectUploadDir, filePath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    /**
     * Get project statistics
     */
    public java.util.Map<String, Object> getProjectStatistics(Long adminId) throws Exception {
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new Exception("Admin not found"));

        List<Project> projects = projectRepository.findByCreatedBy(admin);
        long activeCount = projects.stream()
            .filter(p -> !p.getCompleted()).count();
        long completedCount = projects.stream()
            .filter(Project::getCompleted).count();

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalProjects", projects.size());
        stats.put("activeProjects", activeCount);
        stats.put("completedProjects", completedCount);

        return stats;
    }
    public List<ProjectImage> uploadStageImages(Long stageId, MultipartFile[] files, String[] captions) throws Exception {
        return imageService.uploadStageImages(stageId, files, captions);
    }

    public ProjectImage uploadStageImage(Long stageId, MultipartFile file, String caption) throws Exception {
        return imageService.uploadStageImage(stageId, file, caption);
    }

    public List<ProjectImage> getStageImages(Long stageId) throws Exception {
        return imageService.getStageImages(stageId);
    }

    public List<ProjectImage> getProjectImages(Long projectId) throws Exception {
        return imageService.getProjectImages(projectId);
    }

    public void deleteImage(Long imageId) throws Exception {
        imageService.deleteImage(imageId);
    }

    public ProjectImage updateImageCaption(Long imageId, String caption) throws Exception {
        return imageService.updateImageCaption(imageId, caption);
    }
}
