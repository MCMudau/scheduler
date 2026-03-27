package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.PreviousProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreviousProjectImageRepository extends JpaRepository<PreviousProjectImage, Long> {

    /** All images for a project, in display order. */
    List<PreviousProjectImage> findByPreviousProjectIdOrderByDisplayOrderAscIdAsc(Long projectId);

    /** Fetch the current cover image for a project (there should be at most one). */
    Optional<PreviousProjectImage> findByPreviousProjectIdAndCoverImageTrue(Long projectId);

    /** Count of images attached to a project — useful for admin validation. */
    long countByPreviousProjectId(Long projectId);

    /**
     * Clears the coverImage flag on ALL images of a project before marking
     * a new one as cover. Executed as a single UPDATE, not per-entity.
     * Must be called inside a transaction.
     */
    @Modifying
    @Transactional
    @Query("UPDATE PreviousProjectImage i SET i.coverImage = false WHERE i.previousProject.id = :projectId")
    void clearCoverImageForProject(@Param("projectId") Long projectId);

    /**
     * Look up a specific image by both its own id and its project id.
     * Prevents one project from manipulating images belonging to another.
     */
    Optional<PreviousProjectImage> findByIdAndPreviousProjectId(Long imageId, Long projectId);
}
