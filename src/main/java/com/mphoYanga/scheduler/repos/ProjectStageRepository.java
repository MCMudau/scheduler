package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.Project;
import com.mphoYanga.scheduler.models.ProjectStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectStageRepository extends JpaRepository<ProjectStage, Long> {

    /**
     * Find all stages for a specific project, ordered by stage number
     */
    List<ProjectStage> findByProjectOrderByStageNumber(Project project);

    /**
     * Find a specific stage by project and stage number
     */
    Optional<ProjectStage> findByProjectAndStageNumber(Project project, Integer stageNumber);

    /**
     * Count all stages for a project
     */
    Integer countByProject(Project project);

    /**
     * Find the latest stage for a project
     */
    @Query("SELECT ps FROM ProjectStage ps WHERE ps.project = :project ORDER BY ps.stageNumber DESC LIMIT 1")
    Optional<ProjectStage> findLatestStage(@Param("project") Project project);

    /**
     * Find all stages with their images
     */
    @Query("SELECT ps FROM ProjectStage ps LEFT JOIN FETCH ps.images WHERE ps.project = :project ORDER BY ps.stageNumber")
    List<ProjectStage> findAllWithImages(@Param("project") Project project);

    /**
     * Find stages created by a specific admin
     */
    List<ProjectStage> findByCapturedByOrderByCreatedAtDesc(com.mphoYanga.scheduler.models.Admin admin);

    /**
     * Find stages for a project created after a specific time
     */
    @Query("SELECT ps FROM ProjectStage ps WHERE ps.project = :project AND ps.createdAt >= :timestamp ORDER BY ps.stageNumber DESC")
    List<ProjectStage> findRecentStages(@Param("project") Project project, @Param("timestamp") java.time.LocalDateTime timestamp);
}
