package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.Admin;
import com.mphoYanga.scheduler.models.Client;
import com.mphoYanga.scheduler.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Find all projects created by a specific admin
     */
    List<Project> findByCreatedBy(Admin admin);

    /**
     * Find all projects assigned to a specific client
     */
    List<Project> findByClient(Client client);

    /**
     * Find all ongoing projects
     */
    List<Project> findByStatusAndCompletedFalse(Project.ProjectStatus status);

    /**
     * Find all completed projects
     */
    List<Project> findByCompleted(Boolean completed);

    /**
     * Find all projects by admin and status
     */
    List<Project> findByCreatedByAndStatus(Admin admin, Project.ProjectStatus status);

    /**
     * Find all projects by client and status
     */
    List<Project> findByClientAndStatus(Client client, Project.ProjectStatus status);

    /**
     * Find projects by service type
     */
    List<Project> findByServiceType(String serviceType);

    /**
     * Custom query to find active projects (ongoing and not completed)
     */
    @Query("SELECT p FROM Project p WHERE p.completed = false AND p.status = 'ONGOING' ORDER BY p.createdAt DESC")
    List<Project> findActiveProjects();

    /**
     * Find projects by admin with pagination and filtering
     */
    @Query("SELECT p FROM Project p WHERE p.createdBy = :admin ORDER BY p.createdAt DESC")
    List<Project> findAdminProjects(@Param("admin") Admin admin);

    /**
     * Find all projects with client info
     */
    @Query("SELECT p FROM Project p JOIN FETCH p.client JOIN FETCH p.createdBy ORDER BY p.createdAt DESC")
    List<Project> findAllWithDetails();

    /**
     * Count active projects
     */
    @Query("SELECT COUNT(p) FROM Project p WHERE p.completed = false AND p.status = 'ONGOING'")
    Integer countActiveProjects();

    /**
     * Count projects by admin
     */
    Integer countByCreatedBy(Admin admin);

    /**
     * Find projects by admin and client
     */
    Optional<Project> findByCreatedByAndClient(Admin admin, Client client);

    /**
     * Find recent projects
     */
    @Query(value = "SELECT * FROM projects ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Project> findRecentProjects(@Param("limit") Integer limit);
}
