package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.QuotationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationDocumentRepository extends JpaRepository<QuotationDocument, Long> {

    /** Find the generated PDF document for a specific quotation */
    Optional<QuotationDocument> findByQuotation_QuotationId(Long quotationId);

    /** Find all PDF documents belonging to a specific client */
    List<QuotationDocument> findByClientIdOrderByGeneratedAtDesc(Long clientId);

    /** Check if a confirmed PDF already exists for a quotation */
    boolean existsByQuotation_QuotationId(Long quotationId);
}
