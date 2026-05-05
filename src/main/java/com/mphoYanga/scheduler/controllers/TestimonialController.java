package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.Testimonial;
import com.mphoYanga.scheduler.services.TestimonialService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/testimonials")
@CrossOrigin(origins = "*")
public class TestimonialController {

    private final TestimonialService testimonialService;

    public TestimonialController(TestimonialService testimonialService) {
        this.testimonialService = testimonialService;
    }

    @PostMapping
    public ResponseEntity<?> submit(@RequestParam int rating,
                                    @RequestParam String comment,
                                    HttpSession session) {
        Long clientId = (Long) session.getAttribute("userId");
        if (clientId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not logged in"));
        try {
            Testimonial t = testimonialService.submit(clientId, rating, comment);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Thank you! Your testimonial is pending approval.",
                    "id", t.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to submit testimonial"));
        }
    }

    @GetMapping("/approved")
    public ResponseEntity<?> getApproved() {
        List<Testimonial> list = testimonialService.getApproved();
        return ResponseEntity.ok(list.stream().map(t -> {
            String ini = String.valueOf(t.getClient().getName().charAt(0))
                       + t.getClient().getSurname().charAt(0);
            return Map.of(
                    "id",        t.getId(),
                    "name",      t.getClient().getName() + " " + t.getClient().getSurname(),
                    "initials",  ini.toUpperCase(),
                    "rating",    t.getRating(),
                    "comment",   t.getComment(),
                    "createdAt", t.getCreatedAt().toString()
            );
        }).toList());
    }

    @GetMapping
    public ResponseEntity<?> getAll(HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) adminId = (Long) session.getAttribute("userId");
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        List<Testimonial> list = testimonialService.getAll();
        return ResponseEntity.ok(list.stream().map(t -> {
            String ini = String.valueOf(t.getClient().getName().charAt(0))
                       + t.getClient().getSurname().charAt(0);
            return Map.of(
                    "id",        t.getId(),
                    "name",      t.getClient().getName() + " " + t.getClient().getSurname(),
                    "initials",  ini.toUpperCase(),
                    "rating",    t.getRating(),
                    "comment",   t.getComment(),
                    "status",    t.getStatus().name(),
                    "createdAt", t.getCreatedAt().toString()
            );
        }).toList());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) adminId = (Long) session.getAttribute("userId");
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        try {
            testimonialService.approve(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) adminId = (Long) session.getAttribute("userId");
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        try {
            testimonialService.reject(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpSession session) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) adminId = (Long) session.getAttribute("userId");
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        try {
            testimonialService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
