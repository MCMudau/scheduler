package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.PreviousProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreviousProjectCommentRepository extends JpaRepository<PreviousProjectComment, Long> {

    // ── Public-facing (approved only) ────────────────────────────────────────

    /** Approved comments for a project, oldest first (chronological thread). */
    List<PreviousProjectComment> findByPreviousProjectIdAndApprovedTrueOrderByCreatedAtAsc(Long projectId);

    // ── Admin (all comments regardless of approval) ───────────────────────────

    /** All comments for a project — used in the admin moderation view. */
    List<PreviousProjectComment> findByPreviousProjectIdOrderByCreatedAtDesc(Long projectId);

    /** Pending (unapproved) comments across ALL projects — admin moderation queue. */
    List<PreviousProjectComment> findByApprovedFalseOrderByCreatedAtAsc();

    /** All comments left by a specific client — for the client's own comment history. */
    List<PreviousProjectComment> findByClientIdOrderByCreatedAtDesc(Long clientId);

    // ── Counts ────────────────────────────────────────────────────────────────

    long countByPreviousProjectIdAndApprovedTrue(Long projectId);

    long countByApprovedFalse();

    // ── Duplicate guard ───────────────────────────────────────────────────────

    /**
     * Checks whether a client has already commented on a given project.
     * Service layer can use this to prevent duplicate comments per client per project.
     */
    @Query("SELECT COUNT(c) > 0 FROM PreviousProjectComment c " +
           "WHERE c.previousProject.id = :projectId AND c.client.id = :clientId")
    boolean existsByProjectIdAndClientId(@Param("projectId") Long projectId,
                                          @Param("clientId")  Long clientId);
}
