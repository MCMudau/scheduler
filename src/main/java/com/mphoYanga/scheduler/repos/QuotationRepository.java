package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.Quotation;
import com.mphoYanga.scheduler.models.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    Optional<Quotation> findByQuotationNumber(String quotationNumber);

    List<Quotation> findByClientId(Long clientId);

    List<Quotation> findByClientIdOrderByCreatedAtDesc(Long clientId);

    List<Quotation> findByStatus(QuotationStatus status);

    List<Quotation> findByStatusAndValidUntilBefore(QuotationStatus status, LocalDateTime date);



    long countByStatus(QuotationStatus status);


}