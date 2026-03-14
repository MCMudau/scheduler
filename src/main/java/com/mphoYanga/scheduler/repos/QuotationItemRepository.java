package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.QuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationItemRepository extends JpaRepository<QuotationItem, Long> {

    List<QuotationItem> findByQuotationId(Long quotationId);

    List<QuotationItem> findByQuotationIdOrderByItemNumber(Long quotationId);

    List<QuotationItem> findByServiceType(String serviceType);


    @Query("SELECT SUM(qi.totalPrice) FROM QuotationItem qi WHERE qi.quotation.id = :quotationId")
    Double calculateTotalByQuotationId(@Param("quotationId") Long quotationId);


}