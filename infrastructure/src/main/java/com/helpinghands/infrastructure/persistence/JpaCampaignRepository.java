package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface JpaCampaignRepository extends JpaRepository<Campaign, Long>, JpaSpecificationExecutor<Campaign> {

    List<Campaign> findByStatusAndEndsAtLessThanEqual(CampaignStatus status, Instant endsAt);
}
