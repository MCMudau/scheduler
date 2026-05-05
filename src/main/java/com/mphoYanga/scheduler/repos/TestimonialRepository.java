package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.Testimonial;
import com.mphoYanga.scheduler.models.TestimonialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {
    List<Testimonial> findByStatusOrderByCreatedAtDesc(TestimonialStatus status);
    List<Testimonial> findAllByOrderByCreatedAtDesc();
    List<Testimonial> findByClientIdOrderByCreatedAtDesc(Long clientId);
}
