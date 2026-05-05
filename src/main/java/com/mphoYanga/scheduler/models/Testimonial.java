package com.mphoYanga.scheduler.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "testimonials")
public class Testimonial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestimonialStatus status = TestimonialStatus.PENDING;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() { this.createdAt = LocalDateTime.now(); }

    public Long getId()                          { return id; }

    public Client getClient()                    { return client; }
    public void setClient(Client client)         { this.client = client; }

    public int getRating()                       { return rating; }
    public void setRating(int rating)            { this.rating = rating; }

    public String getComment()                   { return comment; }
    public void setComment(String comment)       { this.comment = comment; }

    public TestimonialStatus getStatus()         { return status; }
    public void setStatus(TestimonialStatus s)   { this.status = s; }

    public LocalDateTime getCreatedAt()          { return createdAt; }
}
