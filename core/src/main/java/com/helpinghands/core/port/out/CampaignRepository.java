package com.helpinghands.core.port.out;

import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignStatus;
import java.util.List;
import java.util.Optional;

public interface CampaignRepository {

    Campaign save(Campaign campaign);

    Optional<Campaign> findById(Long id);

    List<Campaign> findByStatus(CampaignStatus status);
}
