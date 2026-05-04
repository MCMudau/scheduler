package com.mphoYanga.scheduler.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotations")
@Data

public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quotation_id")
    private Long quotationId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "quotation_number", unique = true, nullable = false)
    private String quotationNumber;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "currency")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")

    private QuotationStatus status = QuotationStatus.DRAFT;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)

    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)

    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(
            mappedBy = "quotation",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonManagedReference
    private List<QuotationItem> quotationItems = new ArrayList<>();
    public Quotation(){}

    public Quotation(String quotationNumber, Client client, String title, String description) {
        this.quotationNumber = quotationNumber;
        this.client = client;
        this.title = title;
        this.description = description;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to add quotation item
    public void addQuotationItem(QuotationItem item) {
        item.setQuotation(this);
        this.quotationItems.add(item);
    }

    // Helper method to remove quotation item
    public void removeQuotationItem(QuotationItem item) {
        this.quotationItems.remove(item);
        item.setQuotation(null);
    }

    public Long getId() {
        return quotationId;
    }
}