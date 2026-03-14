package com.mphoYanga.scheduler.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotation_items")
@Data
public class QuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quotation_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    @JsonBackReference
    private Quotation quotation;

    @Column(name = "item_number")
    private Integer itemNumber;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "service_type")
    private String serviceType;

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)

    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")

    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(
            mappedBy = "quotationItem",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonManagedReference
    private List<QuotationItemImage> quotationItemImages = new ArrayList<>();

    public QuotationItem(){}

    public QuotationItem(Quotation quotation, String description, String serviceType, Double quantity, Double unitPrice, Double totalPrice, String notes) {
        this.quotation = quotation;
        this.description = description;
        this.serviceType = serviceType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.notes = notes;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to add quotation item image
    public void addQuotationItemImage(QuotationItemImage image) {
        image.setQuotationItem(this);
        this.quotationItemImages.add(image);
    }

    // Helper method to remove quotation item image
    public void removeQuotationItemImage(QuotationItemImage image) {
        this.quotationItemImages.remove(image);
        image.setQuotationItem(null);
    }
}