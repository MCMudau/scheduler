package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.Activity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
