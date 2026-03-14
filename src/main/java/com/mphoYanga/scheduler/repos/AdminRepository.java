package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin,Long> {
    Optional<Admin> findByEmail(String email);

    /** Check if an email is already registered */
    boolean existsByEmail(String email);
}
