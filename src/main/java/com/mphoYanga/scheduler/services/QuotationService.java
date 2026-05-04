package com.mphoYanga.scheduler.services;

import com.mphoYanga.scheduler.models.*;
import com.mphoYanga.scheduler.repos.ClientRepository;
import com.mphoYanga.scheduler.repos.QuotationItemImageRepository;
import com.mphoYanga.scheduler.repos.QuotationItemRepository;
import com.mphoYanga.scheduler.repos.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final QuotationItemImageRepository quotationItemImageRepository;
    private final ClientRepository clientRepository;

    @Value("${project.upload.dir}")
    private String uploadDir;

    /**
     * Create a new quotation draft
     */
    public Quotation createQuotation(Long clientId, String title, String description) {
        Optional<Client> client = clientRepository.findById(clientId);
        if (client.isEmpty()) {
            throw new IllegalArgumentException("Client not found");
        }

        String quotationNumber = generateQuotationNumber();

        Quotation quotation = new Quotation(
                quotationNumber,
                client.get(),
                title,
                description
        );
        return quotationRepository.save(quotation);
    }

    /**
     * Get all quotations
     */
    @Transactional(readOnly = true)
    public List<Quotation> getAllQuotations() {
        return quotationRepository.findAll();
    }

    /**
     * Get quotations by client ID
     */
    @Transactional(readOnly = true)
    public List<Quotation> getQuotationsByClient(Long clientId) {
        return quotationRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    /**
     * Get quotations by status
     */
    @Transactional(readOnly = true)
    public List<Quotation> getQuotationsByStatus(QuotationStatus status) {
        return quotationRepository.findByStatus(status);
    }

    /**
     * Get a specific quotation by ID
     */
    @Transactional(readOnly = true)
    public Optional<Quotation> getQuotationById(Long quotationId) {
        return quotationRepository.findById(quotationId);
    }

    /**
     * Get quotation by number
     */
    @Transactional(readOnly = true)
    public Optional<Quotation> getQuotationByNumber(String quotationNumber) {
        return quotationRepository.findByQuotationNumber(quotationNumber);
    }

    /**
     * Update quotation status
     */
    public Quotation updateQuotationStatus(Long quotationId, QuotationStatus newStatus, String updatedBy) {
        Optional<Quotation> quotation = quotationRepository.findById(quotationId);
        if (quotation.isEmpty()) {
            throw new IllegalArgumentException("Quotation not found");
        }

        Quotation q = quotation.get();
        q.setStatus(newStatus);
        q.setUpdatedAt(LocalDateTime.now());

        if (newStatus == QuotationStatus.SENT) {
            q.setValidUntil(LocalDateTime.now().plusDays(30));
        }

        return quotationRepository.save(q);
    }

    /**
     * Update quotation details
     */
    public Quotation updateQuotation(Long quotationId, String title, String description) {
        Optional<Quotation> quotation = quotationRepository.findById(quotationId);
        if (quotation.isEmpty()) {
            throw new IllegalArgumentException("Quotation not found");
        }

        Quotation q = quotation.get();
        q.setTitle(title);
        q.setDescription(description);
        q.setUpdatedAt(LocalDateTime.now());

        return quotationRepository.save(q);
    }

    /**
     * Calculate and update total amount for quotation
     */
    public void recalculateTotalAmount(Long quotationId) {
        Optional<Quotation> quotation = quotationRepository.findById(quotationId);
        if (quotation.isEmpty()) {
            throw new IllegalArgumentException("Quotation not found");
        }

        Double total = quotationItemRepository.calculateTotalByQuotationId(quotationId);
        Quotation q = quotation.get();
        q.setTotalAmount(total != null ? total : 0.0);
        quotationRepository.save(q);
    }

    /**
     * Delete quotation
     */
    public void deleteQuotation(Long quotationId) {
        Optional<Quotation> quotation = quotationRepository.findById(quotationId);
        if (quotation.isEmpty()) {
            throw new IllegalArgumentException("Quotation not found");
        }

        Quotation q = quotation.get();
        // Delete all associated images first
        for (QuotationItem item : q.getQuotationItems()) {
            deleteQuotationItemImages(item.getId());
        }
        quotationRepository.delete(q);
    }

    // ==================== QUOTATION ITEM OPERATIONS ====================

    /**
     * Add a new item to quotation
     */
    public QuotationItem addQuotationItem(Long quotationId, String description, String serviceType,
                                          Double quantity, String unitOfMeasure, Double unitPrice, String notes) {

        Optional<Quotation> quotation = quotationRepository.findById(quotationId);
        if (quotation.isEmpty()) {
            throw new IllegalArgumentException("Quotation not found");
        }

        Quotation q = quotation.get();
        int nextItemNumber = q.getQuotationItems().size() + 1;
        Double totalPrice = quantity * unitPrice;

        QuotationItem item = new QuotationItem(
                quotation.get(),
                description,
                serviceType,
                quantity,
                unitPrice,
                totalPrice,
                notes
        );



        QuotationItem savedItem = quotationItemRepository.save(item);
        recalculateTotalAmount(quotationId);

        return savedItem;
    }

    /**
     * Get quotation items by quotation ID
     */
    @Transactional(readOnly = true)
    public List<QuotationItem> getQuotationItems(Long quotationId) {
        return quotationItemRepository.findByQuotationIdOrderByItemNumber(quotationId);
    }

    /**
     * Get a specific quotation item
     */
    @Transactional(readOnly = true)
    public Optional<QuotationItem> getQuotationItem(Long itemId) {
        return quotationItemRepository.findById(itemId);
    }

    /**
     * Update quotation item
     */
    public QuotationItem updateQuotationItem(Long itemId, String description, String serviceType,
                                             Double quantity, String unitOfMeasure, Double unitPrice, String notes) {
        Optional<QuotationItem> item = quotationItemRepository.findById(itemId);
        if (item.isEmpty()) {
            throw new IllegalArgumentException("Quotation item not found");
        }

        QuotationItem qi = item.get();
        qi.setDescription(description);
        qi.setServiceType(serviceType);
        qi.setQuantity(quantity);
        qi.setUnitOfMeasure(unitOfMeasure);
        qi.setUnitPrice(unitPrice);
        qi.setTotalPrice(quantity * unitPrice);
        qi.setNotes(notes);
        qi.setUpdatedAt(LocalDateTime.now());

        QuotationItem updated = quotationItemRepository.save(qi);
        recalculateTotalAmount(qi.getQuotation().getId());

        return updated;
    }

    /**
     * Delete quotation item
     */
    public void deleteQuotationItem(Long itemId) {
        Optional<QuotationItem> item = quotationItemRepository.findById(itemId);
        if (item.isEmpty()) {
            throw new IllegalArgumentException("Quotation item not found");
        }

        QuotationItem qi = item.get();
        Long quotationId = qi.getQuotation().getId();

        // Delete all images associated with this item
        deleteQuotationItemImages(itemId);

        quotationItemRepository.delete(qi);
        recalculateTotalAmount(quotationId);
    }

    // ==================== QUOTATION ITEM IMAGE OPERATIONS ====================

    /**
     * Upload image for quotation item
     */
    public QuotationItemImage uploadQuotationItemImage(Long itemId, MultipartFile file,
                                                       String description, Integer displayOrder,
                                                       String uploadedBy) throws IOException {
        Optional<QuotationItem> item = quotationItemRepository.findById(itemId);
        if (item.isEmpty()) {
            throw new IllegalArgumentException("Quotation item not found");
        }

        QuotationItem qi = item.get();
        Long quotationId = qi.getQuotation().getId();

        // Create directory structure
        Path uploadPath = Paths.get(uploadDir, quotationId.toString(), "item_" + itemId);
        Files.createDirectories(uploadPath);

        // Save file
        String originalFilename = file.getOriginalFilename();
        String filename = System.currentTimeMillis() + "_" + originalFilename;
        Path filePath = uploadPath.resolve(filename);
        Files.write(filePath, file.getBytes());

        // Create image record
        QuotationItemImage image = new QuotationItemImage(
                qi,filename,filePath.toString(),description
        );

        return quotationItemImageRepository.save(image);
    }

    /**
     * Get images for quotation item
     */
    @Transactional(readOnly = true)
    public List<QuotationItemImage> getQuotationItemImages(Long itemId) {
        return quotationItemImageRepository.findByQuotationItemId(itemId);
    }

    /**
     * Delete quotation item image
     */
    public void deleteQuotationItemImage(Long imageId) throws IOException {
        Optional<QuotationItemImage> image = quotationItemImageRepository.findById(imageId);
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Image not found");
        }

        QuotationItemImage qi = image.get();
        Path filePath = Paths.get(qi.getImagePath());

        // Delete from filesystem
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        quotationItemImageRepository.delete(qi);
    }

    /**
     * Delete all images for a quotation item
     */
    public void deleteQuotationItemImages(Long itemId) {
        List<QuotationItemImage> images = quotationItemImageRepository.findByQuotationItemId(itemId);
        for (QuotationItemImage image : images) {
            try {
                Path filePath = Paths.get(image.getImagePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            } catch (IOException e) {
                // Log error but continue
                e.printStackTrace();
            }
        }
        quotationItemImageRepository.deleteByQuotationItemId(itemId);
    }

    /**
     * Update image display order
     */
    public QuotationItemImage updateImageDisplayOrder(Long imageId, Integer displayOrder) {
        Optional<QuotationItemImage> image = quotationItemImageRepository.findById(imageId);
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Image not found");
        }

        QuotationItemImage qi = image.get();

        return quotationItemImageRepository.save(qi);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Generate unique quotation number
     */
    private String generateQuotationNumber() {
        long count = quotationRepository.count();
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        return "QT-" + timestamp + "-" + String.format("%04d", count + 1);
    }

    /**
     * Check if quotation is expired
     */
    public boolean isQuotationExpired(Long quotationId) {
        Optional<Quotation> quotation = quotationRepository.findById(quotationId);
        if (quotation.isEmpty()) {
            return false;
        }

        Quotation q = quotation.get();
        if (q.getValidUntil() == null) {
            return false;
        }

        return LocalDateTime.now().isAfter(q.getValidUntil());
    }

    /**
     * Get quotation statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getQuotationStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("DRAFT", quotationRepository.countByStatus(QuotationStatus.DRAFT));
        stats.put("SENT", quotationRepository.countByStatus(QuotationStatus.SENT));
        stats.put("ACCEPTED", quotationRepository.countByStatus(QuotationStatus.ACCEPTED));
        stats.put("REJECTED", quotationRepository.countByStatus(QuotationStatus.REJECTED));
        stats.put("EXPIRED", quotationRepository.countByStatus(QuotationStatus.EXPIRED));
        stats.put("ARCHIVED", quotationRepository.countByStatus(QuotationStatus.ARCHIVED));
        return stats;
    }



    public QuotationItem getQuotationItemById(Long itemId) {
        return quotationItemRepository.findById(itemId).orElse(null);
    }

    /**
     * Get quotation item image by ID
     */
    public QuotationItemImage getQuotationItemImageById(Long imageId) {
        return quotationItemImageRepository.findById(imageId).orElse(null);
    }

    /**
     * Update quotation item image metadata
     */
    public QuotationItemImage updateQuotationItemImage(Long imageId, String description, Integer displayOrder) {
        QuotationItemImage image = quotationItemImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        if (description != null && !description.trim().isEmpty()) {
            image.setDescription(description);
        }


        return quotationItemImageRepository.save(image);
    }

}