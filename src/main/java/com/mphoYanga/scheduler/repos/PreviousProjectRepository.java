package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.PreviousProject;
import com.mphoYanga.scheduler.models.PreviousProject.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreviousProjectRepository extends JpaRepository<PreviousProject, Long> {

    // ── Public portfolio queries (published only) ─────────────────────────────

    /** All published projects, newest first — used for the client-facing portfolio. */
    List<PreviousProject> findByPublishedTrueOrderByCreatedAtDesc();

    /** Published projects filtered by service category. */
    List<PreviousProject> findByPublishedTrueAndCategoryOrderByCreatedAtDesc(ServiceCategory category);

    // ── Admin queries (all projects regardless of published state) ────────────

    /** Every project for the admin management view, newest first. */
    List<PreviousProject> findAllByOrderByCreatedAtDesc();

    /** Projects by category for admin filtering. */
    List<PreviousProject> findByCategoryOrderByCreatedAtDesc(ServiceCategory category);

    // ── Eager-load variants (avoids N+1 in list views) ────────────────────────

    /**
     * Fetches published projects together with their images in one query.
     * Use this when rendering a gallery list so Hibernate doesn't fire a
     * separate SELECT per project to load images.
     */
    @Query("SELECT DISTINCT p FROM PreviousProject p " +
           "LEFT JOIN FETCH p.images " +
           "WHERE p.published = true " +
           "ORDER BY p.createdAt DESC")
    List<PreviousProject> findPublishedWithImages();

    /**
     * Fetches a single project with all its images and approved comments
     * pre-loaded — use this for the project detail / single-project page.
     * Two separate queries are used (images + comments) because fetching two
     * bag collections in one JPQL join causes a Cartesian explosion.
     */
    @Query("SELECT DISTINCT p FROM PreviousProject p " +
           "LEFT JOIN FETCH p.images " +
           "WHERE p.id = :id AND p.published = true")
    java.util.Optional<PreviousProject> findPublishedByIdWithImages(@Param("id") Long id);

    // ── Counts ────────────────────────────────────────────────────────────────

    long countByPublishedTrue();

    long countByCategory(ServiceCategory category);
}
