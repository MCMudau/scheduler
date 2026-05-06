package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.*;
import com.mphoYanga.scheduler.services.ActivityService;
import com.mphoYanga.scheduler.services.QuotationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/quotations")
@CrossOrigin(origins = "*")
public class QuotationRestController {

    @Autowired
    private QuotationService quotationService;

    @Autowired
    private ActivityService activityService;

    // ==================== QUOTATION ENDPOINTS ====================

    /**
     * Create a new quotation draft
     */
    @PostMapping("/create")
    public ResponseEntity<?> createQuotation(@RequestParam String title,
                                             @RequestParam(required = false) String description,
                                             HttpSession session) {


        try {
            Long clientId = (Long) session.getAttribute("userId");
            Quotation quotation = quotationService.createQuotation(clientId, title, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(quotation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create quotation"));
        }
    }

    /**
     * Get all quotations
     */
    @GetMapping
    public ResponseEntity<?> getAllQuotations() {
        try {
            List<Quotation> quotations = quotationService.getAllQuotations();
            return ResponseEntity.ok(Map.of("success", true, "data", quotations));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to fetch quotations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get quotations by client ID
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getQuotationsByClient(@PathVariable Long clientId) {
        try {
            List<Quotation> quotations = quotationService.getQuotationsByClient(clientId);
            return ResponseEntity.ok(quotations);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch quotations"));
        }
    }

    /**
     * Get quotations by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getQuotationsByStatus(@PathVariable String status) {
        try {
            QuotationStatus statusEnum = QuotationStatus.valueOf(status.toUpperCase());
            List<Quotation> quotations = quotationService.getQuotationsByStatus(statusEnum);
            return ResponseEntity.ok(quotations);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch quotations"));
        }
    }

    /**
     * Get quotation by ID
     */
    @GetMapping("/{quotationId}")
    public ResponseEntity<?> getQuotationById(@PathVariable Long quotationId) {
        try {
            Optional<Quotation> quotation = quotationService.getQuotationById(quotationId);
            if (quotation.isPresent()) {
                return ResponseEntity.ok(quotation.get());
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch quotation"));
        }
    }

    /**
     * Update quotation details
     */
    @PutMapping("/{quotationId}/update")
    public ResponseEntity<?> updateQuotation(@PathVariable Long quotationId,
                                             @RequestParam String title,
                                             @RequestParam(required = false) String description) {
        try {
            Quotation quotation = quotationService.updateQuotation(quotationId, title, description);
            return ResponseEntity.ok(quotation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update quotation"));
        }
    }

    /**
     * Update quotation status
     */
    @PutMapping("/{quotationId}/status")
    public ResponseEntity<?> updateQuotationStatus(@PathVariable Long quotationId,
                                                   @RequestParam String status,
                                                   @RequestParam(required = false) String updatedBy) {
        try {
            QuotationStatus statusEnum = QuotationStatus.valueOf(status.toUpperCase());
            Quotation quotation = quotationService.updateQuotationStatus(quotationId, statusEnum, updatedBy);

            if (statusEnum == QuotationStatus.SENT && quotation.getClient() != null) {
                Client c = quotation.getClient();
                String clientName = c.getName() + " " + c.getSurname();
                activityService.log(
                        c.getId(), clientName,
                        Activity.ActorType.CLIENT,
                        "Submitted quotation: " + quotation.getTitle(),
                        "QUOTATION", quotation.getQuotationId()
                );
            }

            return ResponseEntity.ok(quotation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update status"));
        }
    }

    /**
     * Delete quotation
     */
    @DeleteMapping("/{quotationId}")
    public ResponseEntity<?> deleteQuotation(@PathVariable Long quotationId) {
        try {
            quotationService.deleteQuotation(quotationId);
            return ResponseEntity.ok(Map.of("message", "Quotation deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete quotation"));
        }
    }

    // ==================== QUOTATION ITEM ENDPOINTS ====================

    /**
     * Add item to quotation
     */
    @PostMapping("/{quotationId}/items")
    public ResponseEntity<?> addQuotationItem(@PathVariable Long quotationId,
                                              @RequestParam String description,
                                              @RequestParam(required = false) String serviceType,
                                              @RequestParam Double quantity,
                                              @RequestParam(required = false) String unitOfMeasure,
                                              @RequestParam Double unitPrice,
                                              @RequestParam(required = false) String notes) {
        try {
            QuotationItem item = quotationService.addQuotationItem(quotationId, description, serviceType,
                    quantity, unitOfMeasure, unitPrice, notes);
            return ResponseEntity.status(HttpStatus.CREATED).body(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to add item"));
        }
    }


    /**
     * Get items for quotation
     */
    @GetMapping("/{quotationId}/items")
    public ResponseEntity<?> getQuotationItems(@PathVariable Long quotationId) {
        try {
            List<QuotationItem> items = quotationService.getQuotationItems(quotationId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch items"));
        }
    }

    /**
     * Update quotation item
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateQuotationItem(@PathVariable Long itemId,
                                                 @RequestParam String description,
                                                 @RequestParam(required = false) String serviceType,
                                                 @RequestParam Double quantity,
                                                 @RequestParam(required = false) String unitOfMeasure,
                                                 @RequestParam Double unitPrice,
                                                 @RequestParam(required = false) String notes) {
        try {
            QuotationItem item = quotationService.updateQuotationItem(itemId, description, serviceType,
                    quantity, unitOfMeasure, unitPrice, notes);
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update item"));
        }
    }

    /**
     * Delete quotation item
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> deleteQuotationItem(@PathVariable Long itemId) {
        try {
            quotationService.deleteQuotationItem(itemId);
            return ResponseEntity.ok(Map.of("message", "Item deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete item"));
        }
    }

    // ==================== QUOTATION ITEM IMAGE ENDPOINTS ====================

    /**
     * Upload image for quotation item
     */
    @PostMapping("/items/{itemId}/images")
    public ResponseEntity<?> uploadImage(@PathVariable Long itemId,
                                         @RequestParam MultipartFile file,
                                         @RequestParam(required = false) String description,
                                         @RequestParam(required = false) Integer displayOrder,
                                         @RequestParam(required = false) String uploadedBy) {
        try {
            QuotationItemImage image = quotationService.uploadQuotationItemImage(itemId, file, description, displayOrder, uploadedBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(image);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload image"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload image"));
        }
    }

    /**
     * Get images for quotation item
     */
    @GetMapping("/items/{itemId}/images")
    public ResponseEntity<?> getQuotationItemImages(@PathVariable Long itemId) {
        try {
            List<QuotationItemImage> images = quotationService.getQuotationItemImages(itemId);
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch images"));
        }
    }

    /**
     * Delete quotation item image
     */
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<?> deleteImage(@PathVariable Long imageId) {
        try {
            quotationService.deleteQuotationItemImage(imageId);
            return ResponseEntity.ok(Map.of("message", "Image deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete image"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete image"));
        }
    }



    // ==================== STATISTICS ENDPOINTS ====================

    /**
     * Get quotation statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getQuotationStats() {
        try {
            Map<String, Long> stats = quotationService.getQuotationStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch stats"));
        }
    }


    @GetMapping("/items/{itemId}/details")
    public ResponseEntity<?> getQuotationItemDetails(@PathVariable Long itemId) {
        try {
            QuotationItem item = quotationService.getQuotationItemById(itemId);
            if (item == null) {
                return ResponseEntity.notFound().build();
            }

            // Build comprehensive response with item details and images
            Map<String, Object> itemDetails = new HashMap<>();
            itemDetails.put("id", item.getId());
            itemDetails.put("quotationId", item.getQuotation().getId());
            itemDetails.put("description", item.getDescription());
            itemDetails.put("serviceType", item.getServiceType());
            itemDetails.put("quantity", item.getQuantity());
            itemDetails.put("unitOfMeasure", item.getUnitOfMeasure());
            itemDetails.put("unitPrice", item.getUnitPrice());
            itemDetails.put("notes", item.getNotes());

            // Calculate total cost
            Double totalCost = item.getQuantity() * item.getUnitPrice();
            itemDetails.put("totalCost", totalCost);

            // Get associated images
            List<QuotationItemImage> images = quotationService.getQuotationItemImages(itemId);
            itemDetails.put("images", images);
            itemDetails.put("imageCount", images.size());

            // Add quotation reference
            Quotation quotation = item.getQuotation();
            Map<String, Object> quotationRef = new HashMap<>();
            quotationRef.put("id", quotation.getId());
            quotationRef.put("title", quotation.getTitle());
            quotationRef.put("description", quotation.getDescription());
            itemDetails.put("quotation", quotationRef);

            return ResponseEntity.ok(itemDetails);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch item details", "details", e.getMessage()));
        }


    }


    @GetMapping("/images/{imageId}")
    public ResponseEntity<?> getQuotationItemImage(@PathVariable Long imageId) {
        try {
            QuotationItemImage image = quotationService.getQuotationItemImageById(imageId);
            if (image == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> imageDetails = new HashMap<>();
            imageDetails.put("id", image.getId());
            imageDetails.put("itemId", image.getQuotationItem().getId());
            imageDetails.put("imagePath", image.getImagePath());
            imageDetails.put("description", image.getDescription());


            return ResponseEntity.ok(imageDetails);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch image"));
        }
    }


    @PutMapping("/images/{imageId}")
    public ResponseEntity<?> updateImage(@PathVariable Long imageId,
                                         @RequestParam(required = false) String description,
                                         @RequestParam(required = false) Integer displayOrder) {
        try {
            QuotationItemImage image = quotationService.updateQuotationItemImage(imageId, description, displayOrder);
            return ResponseEntity.ok(image);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update image"));
        }
    }


    @GetMapping("/{quotationId}/items/stats")
    public ResponseEntity<?> getQuotationItemStats(@PathVariable Long quotationId) {
        try {
            List<QuotationItem> items = quotationService.getQuotationItems(quotationId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalItems", items.size());
            stats.put("totalQuantity", items.stream().mapToDouble(QuotationItem::getQuantity).sum());
            stats.put("totalValue", items.stream()
                    .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                    .sum());
            stats.put("averageUnitPrice", items.isEmpty() ? 0 :
                    items.stream().mapToDouble(QuotationItem::getUnitPrice).average().orElse(0));

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch item stats"));
        }
    }


}