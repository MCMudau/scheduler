package com.mphoYanga.scheduler.services;

import com.mphoYanga.scheduler.models.Client;
import com.mphoYanga.scheduler.models.Testimonial;
import com.mphoYanga.scheduler.models.TestimonialStatus;
import com.mphoYanga.scheduler.repos.ClientRepository;
import com.mphoYanga.scheduler.repos.TestimonialRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestimonialService {

    private final TestimonialRepository testimonialRepository;
    private final ClientRepository clientRepository;

    public TestimonialService(TestimonialRepository testimonialRepository,
                              ClientRepository clientRepository) {
        this.testimonialRepository = testimonialRepository;
        this.clientRepository = clientRepository;
    }

    public Testimonial submit(Long clientId, int rating, String comment) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        Testimonial t = new Testimonial();
        t.setClient(client);
        t.setRating(rating);
        t.setComment(comment.trim());
        return testimonialRepository.save(t);
    }

    public List<Testimonial> getApproved() {
        return testimonialRepository.findByStatusOrderByCreatedAtDesc(TestimonialStatus.APPROVED);
    }

    public List<Testimonial> getAll() {
        return testimonialRepository.findAllByOrderByCreatedAtDesc();
    }

    public Testimonial approve(Long id) {
        Testimonial t = testimonialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Testimonial not found"));
        t.setStatus(TestimonialStatus.APPROVED);
        return testimonialRepository.save(t);
    }

    public Testimonial reject(Long id) {
        Testimonial t = testimonialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Testimonial not found"));
        t.setStatus(TestimonialStatus.REJECTED);
        return testimonialRepository.save(t);
    }

    public void delete(Long id) {
        testimonialRepository.deleteById(id);
    }
}
