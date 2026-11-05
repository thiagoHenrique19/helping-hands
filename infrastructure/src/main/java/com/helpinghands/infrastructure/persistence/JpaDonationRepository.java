package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.Donation;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaDonationRepository extends JpaRepository<Donation, Long> {

    Page<Donation> findByCampaignId(Long campaignId, Pageable pageable);

    @Query(value = """
            SELECT (array_agg(d.donor_name ORDER BY d.created_at DESC))[1] AS donorName,
                   SUM(d.amount)                                           AS totalAmount,
                   COUNT(*)                                                AS donationsCount
            FROM donation d
            WHERE d.campaign_id = :campaignId
              AND d.anonymous = FALSE
            GROUP BY lower(d.donor_email)
            ORDER BY SUM(d.amount) DESC, MIN(d.created_at) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopDonorProjection> findTopDonors(@Param("campaignId") Long campaignId, @Param("limit") int limit);

    interface TopDonorProjection {

        String getDonorName();

        BigDecimal getTotalAmount();

        Long getDonationsCount();
    }
}
