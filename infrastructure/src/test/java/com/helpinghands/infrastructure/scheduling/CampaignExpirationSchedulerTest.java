package com.helpinghands.infrastructure.scheduling;

import static org.mockito.Mockito.verify;

import com.helpinghands.core.port.in.ExpireCampaignsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignExpirationSchedulerTest {

    @Mock private ExpireCampaignsUseCase expireCampaignsUseCase;

    @Test
    void shouldDelegateToExpireCampaignsUseCase() {
        new CampaignExpirationScheduler(expireCampaignsUseCase).expireOverdueCampaigns();

        verify(expireCampaignsUseCase).expireOverdueCampaigns();
    }
}
