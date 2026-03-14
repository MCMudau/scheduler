package com.mphoYanga.scheduler.repos;

import com.mphoYanga.scheduler.models.QuotationItemImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationItemImageRepository extends JpaRepository<QuotationItemImage, Long> {

    List<QuotationItemImage> findByQuotationItemId(Long quotationItemId);

    void deleteByQuotationItemId(Long quotationItemId);
}