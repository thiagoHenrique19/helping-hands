package com.helpinghands.infrastructure.scheduling;

import com.helpinghands.core.port.in.ExpireCampaignsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CampaignExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(CampaignExpirationScheduler.class);

    private final ExpireCampaignsUseCase expireCampaignsUseCase;

    public CampaignExpirationScheduler(ExpireCampaignsUseCase expireCampaignsUseCase) {
        this.expireCampaignsUseCase = expireCampaignsUseCase;
    }

    @Scheduled(cron = "${helpinghands.campaigns.expiration-cron:0 */5 * * * *}")
    public void expireOverdueCampaigns() {
        int expired = expireCampaignsUseCase.expireOverdueCampaigns();
        if (expired > 0) {
            log.info("Expired {} overdue campaign(s)", expired);
        }
    }
}
