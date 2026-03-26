package com.mphoYanga.scheduler.controllers;

import com.mphoYanga.scheduler.models.Quotation;
import com.mphoYanga.scheduler.models.QuotationDocument;
import com.mphoYanga.scheduler.models.QuotationStatus;
import com.mphoYanga.scheduler.repos.QuotationDocumentRepository;
import com.mphoYanga.scheduler.services.EmailService;
import com.mphoYanga.scheduler.services.QuotationPdfService;
import com.mphoYanga.scheduler.services.QuotationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles all PDF-related quotation endpoints.
 *
 * ── ENDPOINTS ──────────────────────────────────────────────────────────────
 *
 *  ADMIN
 *  GET  /api/quotations/{id}/pdf
 *       Stream the PDF inline (preview) or as a download.
 *       While the quotation is still being reviewed this regenerates the PDF
 *       on the fly from current data.  After confirmation it serves the saved
 *       file from disk.
 *       Params: inline=true|false (default false → Save-As dialog)
 *
 *  POST /api/quotations/{id}/confirm
 *       Confirm the quotation: saves it as ACCEPTED, generates + saves the PDF
 *       to disk, records a QuotationDocument row, and emails the client with
 *       the PDF attached.
 *       Body param: adminId (Long) — the confirming admin's session ID
 *
 *  CLIENT
 *  GET  /api/quotations/my-documents
 *       Returns all QuotationDocument records for the session client (metadata
 *       only — no bytes).  Use to render a "My Confirmed Quotations" list.
 *
 *  GET  /api/quotations/documents/{documentId}/download
 *       Download the PDF for a specific document.  Only works if the
 *       document belongs to the session client (ownership check).
 *
 *  GET  /api/quotations/{quotationId}/document
 *       Shortcut: look up the document by quotation ID and download it.
 *       Useful for the client dashboard "Download Quote" button.
 */
@RestController
@RequestMapping("/api/quotations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class QuotationPdfController {

    private final QuotationService             quotationService;
    private final QuotationPdfService          quotationPdfService;
    private final QuotationDocumentRepository  documentRepository;
    private final EmailService                 emailService;

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN — PREVIEW / GENERATE ON THE FLY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Stream or download the PDF for a quotation.
     * Before confirmation: regenerates from current data every time.
     * After confirmation: serves the saved file from disk.
     */
    @GetMapping("/{quotationId}/pdf")
    public ResponseEntity<byte[]> streamQuotationPdf(
            @PathVariable Long quotationId,
            @RequestParam(defaultValue = "false") boolean inline) {

        Optional<Quotation> opt = quotationService.getQuotationById(quotationId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Quotation quotation = opt.get();

        // Populate items safely — addAll into the existing managed collection
        // rather than replacing it with setQuotationItems()
        quotation.getQuotationItems().clear();
        quotation.getQuotationItems().addAll(quotationService.getQuotationItems(quotationId));

        try {
            byte[] pdfBytes;

            // If already confirmed, serve the saved file so bytes are identical
            // to what was emailed and stored
            Optional<QuotationDocument> savedDoc =
                    documentRepository.findByQuotation_QuotationId(quotationId);

            if (savedDoc.isPresent()) {
                pdfBytes = quotationPdfService.loadPdfBytes(savedDoc.get());
            } else {
                // Still in review — generate fresh from current data
                pdfBytes = quotationPdfService.generateQuotationPdf(quotation);
            }

            return buildPdfResponse(pdfBytes,
                    "Quotation_" + quotation.getQuotationNumber() + ".pdf", inline);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN — CONFIRM QUOTATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Confirm the quotation:
     *  1. Set status → ACCEPTED (with validUntil = now + 30 days)
     *  2. Generate + save PDF to disk
     *  3. Persist QuotationDocument record
     *  4. Email the client with the PDF attached
     */
    @PostMapping("/{quotationId}/confirm")
    public ResponseEntity<?> confirmQuotation(
            @PathVariable Long quotationId,
            HttpSession session) {

        Long adminId = (Long) session.getAttribute("userId");
        if (adminId == null) adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Admin session not found"));
        }

        Optional<Quotation> opt = quotationService.getQuotationById(quotationId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            // ── STEP 1: Update status in its own transaction ──────────────────
            // Must happen BEFORE we touch the items collection.
            // Never call setQuotationItems() on the managed Quotation entity —
            // Hibernate's orphanRemoval will throw if the collection reference
            // is replaced on a managed object mid-transaction.
            quotationService.updateQuotationStatus(quotationId, QuotationStatus.ACCEPTED,
                    "admin:" + adminId);

            // ── STEP 2: Load a FRESH snapshot for the PDF service ─────────────
            // After the status commit, re-fetch a clean entity. Then populate
            // the items list using the plain setter — this is now a detached
            // object used only for PDF rendering, never re-saved by JPA.
            Quotation quotation = quotationService.getQuotationById(quotationId)
                    .orElseThrow(() -> new IllegalStateException("Quotation disappeared after status update"));

            // Populate items directly — safe because this entity instance is
            // used only for read/PDF generation below, never merged back to DB.
            quotation.getQuotationItems().clear();
            quotation.getQuotationItems().addAll(quotationService.getQuotationItems(quotationId));

            // ── STEP 3: Generate + save PDF, record in DB ─────────────────────
            QuotationDocument doc =
                    quotationPdfService.generateAndSave(quotation, adminId);

            // ── STEP 4: Email the client asynchronously ───────────────────────
            try {
                String clientName  = quotation.getClient().getName();
                String clientEmail = quotation.getClient().getEmail();
                String totalStr    = "ZWL " + String.format("%.2f",
                        quotation.getQuotationItems().stream()
                                .mapToDouble(i -> i.getTotalPrice() != null ? i.getTotalPrice() : 0)
                                .sum());

                byte[] pdfBytes = quotationPdfService.loadPdfBytes(doc);

                emailService.sendQuotationConfirmed(
                        clientEmail,
                        clientName,
                        quotation.getQuotationNumber(),
                        quotation.getTitle() != null ? quotation.getTitle() : "Construction Project",
                        totalStr,
                        pdfBytes,
                        doc.getFileName()
                );

                quotationPdfService.markEmailSent(doc.getDocumentId());

            } catch (Exception emailErr) {
                // Email failure must not roll back the confirmation
                System.err.println("[QuotationPdfController] Email failed: " + emailErr.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    "success",    true,
                    "message",    "Quotation confirmed and client notified",
                    "documentId", doc.getDocumentId(),
                    "fileName",   doc.getFileName(),
                    "emailSent",  doc.getEmailSent()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to confirm quotation: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLIENT — LIST MY CONFIRMED DOCUMENTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all confirmed QuotationDocument records for the logged-in client.
     * Call this to populate a "My Confirmed Quotations" list in the client portal.
     */
    @GetMapping("/my-documents")
    public ResponseEntity<?> getMyDocuments(HttpSession session) {
        Long clientId = (Long) session.getAttribute("userId");
        if (clientId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Client session not found"));
        }

        List<QuotationDocument> docs =
                documentRepository.findByClientIdOrderByGeneratedAtDesc(clientId);

        // Return lightweight metadata — no bytes
        List<Map<String, Object>> response = docs.stream().map(doc -> {
            Quotation q = doc.getQuotation();
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("documentId",     doc.getDocumentId());
            m.put("quotationId",    q.getQuotationId());
            m.put("quotationNumber",q.getQuotationNumber());
            m.put("title",          q.getTitle());
            m.put("totalAmount",    q.getTotalAmount());
            m.put("currency",       q.getCurrency() != null ? q.getCurrency() : "ZWL");
            m.put("fileName",       doc.getFileName());
            m.put("fileSizeBytes",  doc.getFileSizeBytes());
            m.put("generatedAt",    doc.getGeneratedAt());
            m.put("emailSent",      doc.getEmailSent());
            return m;
        }).toList();

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLIENT — DOWNLOAD BY DOCUMENT ID
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Download a confirmed PDF by its QuotationDocument ID.
     * Ownership is verified against the session client — a client cannot
     * download another client's document.
     */
    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocumentById(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean inline,
            HttpSession session) {

        Long clientId = (Long) session.getAttribute("userId");
        if (clientId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<QuotationDocument> opt = documentRepository.findById(documentId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        QuotationDocument doc = opt.get();

        // Ownership check
        if (!doc.getClientId().equals(clientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            byte[] pdfBytes = quotationPdfService.loadPdfBytes(doc);
            return buildPdfResponse(pdfBytes, doc.getFileName(), inline);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLIENT — DOWNLOAD BY QUOTATION ID (shortcut)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shortcut: look up the confirmed document by quotation ID and download it.
     * Handy for the client dashboard "Download Quote" button where you have the
     * quotation ID readily available.
     */
    @GetMapping("/{quotationId}/document/download")
    public ResponseEntity<byte[]> downloadByQuotationId(
            @PathVariable Long quotationId,
            @RequestParam(defaultValue = "false") boolean inline,
            HttpSession session) {

        Long clientId = (Long) session.getAttribute("userId");
        if (clientId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<QuotationDocument> opt =
                documentRepository.findByQuotation_QuotationId(quotationId);

        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        QuotationDocument doc = opt.get();

        if (!doc.getClientId().equals(clientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            byte[] pdfBytes = quotationPdfService.loadPdfBytes(doc);
            return buildPdfResponse(pdfBytes, doc.getFileName(), inline);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> buildPdfResponse(byte[] bytes, String filename, boolean inline) {
        String disposition = (inline ? "inline" : "attachment") + "; filename=\"" + filename + "\"";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition);
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}