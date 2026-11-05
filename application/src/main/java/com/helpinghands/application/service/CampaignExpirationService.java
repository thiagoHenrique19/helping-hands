package com.helpinghands.application.service;

import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.port.in.ExpireCampaignsUseCase;
import com.helpinghands.core.port.out.CampaignRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignExpirationService implements ExpireCampaignsUseCase {

    private final CampaignRepository campaignRepository;
    private final Clock clock;

    public CampaignExpirationService(CampaignRepository campaignRepository, Clock clock) {
        this.campaignRepository = campaignRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int expireOverdueCampaigns() {
        Instant now = clock.instant();
        int expired = 0;
        for (Campaign campaign : campaignRepository.findActiveWithDeadlineUpTo(now)) {
            if (campaign.expireIfOverdue(now)) {
                campaignRepository.save(campaign);
                expired++;
            }
        }
        return expired;
    }
}
