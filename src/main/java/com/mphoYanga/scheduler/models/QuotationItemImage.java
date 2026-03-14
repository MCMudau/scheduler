package com.mphoYanga.scheduler.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotation_item_images")
@Data

public class QuotationItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_item_id", nullable = false)
    @JsonBackReference
    private QuotationItem quotationItem;

    @Column(name = "image_filename", nullable = false)
    private String imageFilename;

    @Column(name = "image_path", nullable = false)
    private String imagePath;



    @Column(name = "description", columnDefinition = "TEXT")
    private String description;


    public QuotationItemImage(){}
    public QuotationItemImage(QuotationItem quotationItem, String imageFilename, String imagePath, String description) {
        this.quotationItem = quotationItem;
        this.imageFilename = imageFilename;
        this.imagePath = imagePath;
        this.description = description;
    }

    public QuotationItem getQuotationItem() {
        return quotationItem;
    }

    public void setQuotationItem(QuotationItem quotationItem) {
        this.quotationItem = quotationItem;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public void setImageFilename(String imageFilename) {
        this.imageFilename = imageFilename;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getId() {
        return this.imageId;
    }
}