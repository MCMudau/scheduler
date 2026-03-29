package com.mphoYanga.scheduler.services;

import com.mphoYanga.scheduler.models.ProjectImage;
import com.mphoYanga.scheduler.models.ProjectStage;
import com.mphoYanga.scheduler.repos.ProjectImageRepository;
import com.mphoYanga.scheduler.repos.ProjectStageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ImageService {

    @Autowired
    private ProjectImageRepository projectImageRepository;

    @Autowired
    private ProjectStageRepository projectStageRepository;

    @Value("${project.upload.dir}")
    private String projectUploadDir;

    /**
     * Upload a single image for a project stage
     */
    public ProjectImage uploadStageImage(Long stageId, MultipartFile file, String caption) throws Exception {
        ProjectStage stage = projectStageRepository.findById(stageId)
                .orElseThrow(() -> new Exception("Stage not found"));

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String stageDirPath = "projects/"+ stage.getProject().getId() + "/stage_" + stage.getStageNumber() + "/";
        String filePath = stageDirPath + fileName;

        createImageDirectory(stageDirPath);

        Path fullPath = Paths.get(projectUploadDir, filePath);
        Files.write(fullPath, file.getBytes());

        ProjectImage image = new ProjectImage(
                stage,
                file.getOriginalFilename(),
                filePath,
                file.getSize(),
                file.getContentType()
        );
        image.setCaption(caption);

        return projectImageRepository.save(image);
    }

    /**
     * Upload multiple images for a project stage
     */
    public List<ProjectImage> uploadStageImages(Long stageId, MultipartFile[] files, String[] captions) throws Exception {
        ProjectStage stage = projectStageRepository.findById(stageId)
                .orElseThrow(() -> new Exception("Stage not found"));

        List<ProjectImage> uploadedImages = new ArrayList<>();

        if (files == null || files.length == 0) {
            throw new Exception("No files provided");
        }

        String stageDirPath = "projects/"+ stage.getProject().getId() + "/stage_" + stage.getStageNumber() + "/";
        createImageDirectory(stageDirPath);

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String filePath = stageDirPath + fileName;

            Path fullPath = Paths.get(projectUploadDir, filePath);
            Files.write(fullPath, file.getBytes());

            ProjectImage image = new ProjectImage(
                    stage,
                    file.getOriginalFilename(),
                    filePath,
                    file.getSize(),
                    file.getContentType()
            );

            if (captions != null && i < captions.length && captions[i] != null) {
                image.setCaption(captions[i]);
            }

            uploadedImages.add(projectImageRepository.save(image));
        }

        return uploadedImages;
    }

    /**
     * Get all images for a specific stage
     */
    public List<ProjectImage> getStageImages(Long stageId) throws Exception {
        ProjectStage stage = projectStageRepository.findById(stageId)
                .orElseThrow(() -> new Exception("Stage not found"));

        return projectImageRepository.findByStageOrderByUploadedAtDesc(stage);
    }

    /**
     * Get all images for a project
     */
    public List<ProjectImage> getProjectImages(Long projectId) throws Exception {
        return projectImageRepository.findByProjectId(projectId);
    }

    /**
     * Delete a specific image
     */
    public void deleteImage(Long imageId) throws Exception {
        ProjectImage image = projectImageRepository.findById(imageId)
                .orElseThrow(() -> new Exception("Image not found"));

        try {
            deleteImageFile(image.getFilePath());
        } catch (IOException e) {
            System.err.println("Failed to delete file from disk: " + e.getMessage());
        }

        projectImageRepository.deleteById(imageId);
    }

    /**
     * Update image caption
     */
    public ProjectImage updateImageCaption(Long imageId, String caption) throws Exception {
        ProjectImage image = projectImageRepository.findById(imageId)
                .orElseThrow(() -> new Exception("Image not found"));

        image.setCaption(caption);
        return projectImageRepository.save(image);
    }

    /**
     * Create image directory if not exists
     */
    private void createImageDirectory(String stageDirPath) throws IOException {
        Path path = Paths.get(projectUploadDir, stageDirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Delete image file from disk
     */
    private void deleteImageFile(String filePath) throws IOException {
        Path path = Paths.get(projectUploadDir, filePath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }
}
