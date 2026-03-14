package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.ProjectImage;
import com.mphoYanga.scheduler.models.ProjectStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {

    /**
     * Find all images for a specific stage
     */
    List<ProjectImage> findByStageOrderByUploadedAtDesc(ProjectStage stage);

    /**
     * Count all images for a stage
     */
    Integer countByStage(ProjectStage stage);

    /**
     * Find images for a project (across all stages)
     */
    @Query("SELECT pi FROM ProjectImage pi WHERE pi.stage.project.id = :projectId ORDER BY pi.uploadedAt DESC")
    List<ProjectImage> findByProjectId(@Param("projectId") Long projectId);

    /**
     * Find all images with stage and project details
     */
    @Query("SELECT pi FROM ProjectImage pi JOIN FETCH pi.stage ps WHERE ps.id = :stageId ORDER BY pi.uploadedAt DESC")
    List<ProjectImage> findStageImagesWithDetails(@Param("stageId") Long stageId);

    /**
     * Count total images for a project
     */
    @Query("SELECT COUNT(pi) FROM ProjectImage pi WHERE pi.stage.project.id = :projectId")
    Integer countImagesByProject(@Param("projectId") Long projectId);

    /**
     * Find recent images (last N images across all projects)
     */
    @Query(value = "SELECT * FROM project_images ORDER BY uploaded_at DESC LIMIT :limit", nativeQuery = true)
    List<ProjectImage> findRecentImages(@Param("limit") Integer limit);

    /**
     * Find images by content type (e.g., "image/jpeg")
     */
    List<ProjectImage> findByContentType(String contentType);
}
