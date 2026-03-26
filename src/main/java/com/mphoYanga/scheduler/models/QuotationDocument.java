package com.mphoYanga.scheduler.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Represents a generated, finalised PDF quotation document.
 *
 * One record is created per confirmed quotation. Stores the path to the
 * saved PDF file on disk so both the admin and the client can download it
 * at any time without regenerating it.
 *
 * Table: quotation_documents
 */
@Entity
@Table(name = "quotation_documents")
@Data
public class QuotationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    /** The quotation this document belongs to (one-to-one in practice) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false, unique = true)
    private Quotation quotation;

    /** ID of the admin who confirmed and generated this document */
    @Column(name = "confirmed_by_admin_id", nullable = false)
    private Long confirmedByAdminId;

    /** ID of the client who owns the quotation (denormalised for fast lookup) */
    @Column(name = "client_id", nullable = false)
    private Long clientId;

    /**
     * Relative path to the saved PDF file from the application root.
     * Example: quotations/QT-123456-0001.pdf
     * Serve it under the /quotations/** static resource mapping.
     */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    /** Original filename shown to the user on download */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** File size in bytes — useful for showing in the UI */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /** When the admin confirmed and the PDF was generated */
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    /** Whether the confirmation email was successfully sent to the client */
    @Column(name = "email_sent")
    private Boolean emailSent = false;

    public QuotationDocument() {}

    public QuotationDocument(Quotation quotation, Long confirmedByAdminId, Long clientId,
                             String filePath, String fileName, Long fileSizeBytes) {
        this.quotation         = quotation;
        this.confirmedByAdminId = confirmedByAdminId;
        this.clientId          = clientId;
        this.filePath          = filePath;
        this.fileName          = fileName;
        this.fileSizeBytes     = fileSizeBytes;
    }
}
