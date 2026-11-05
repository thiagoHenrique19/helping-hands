package com.helpinghands.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignCategory;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.port.out.CampaignRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignExpirationServiceTest {

    @Mock private CampaignRepository campaignRepository;

    private final Organization organization = Organization.register("Org", "o@org.org", "d");

    @Test
    void shouldExpireOverdueCampaigns() {
        Instant endsAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant now = endsAt.plus(1, ChronoUnit.HOURS);
        Campaign overdue = Campaign.create(organization, "Late", "d", BigDecimal.TEN, endsAt, CampaignCategory.FOOD);
        when(campaignRepository.findActiveWithDeadlineUpTo(now)).thenReturn(List.of(overdue));
        var service = new CampaignExpirationService(campaignRepository, Clock.fixed(now, ZoneOffset.UTC));

        int expired = service.expireOverdueCampaigns();

        assertThat(expired).isEqualTo(1);
        assertThat(overdue.getStatus()).isEqualTo(CampaignStatus.EXPIRED);
        verify(campaignRepository).save(overdue);
    }

    @Test
    void shouldDoNothingWhenNothingIsOverdue() {
        Instant now = Instant.now();
        when(campaignRepository.findActiveWithDeadlineUpTo(now)).thenReturn(List.of());
        var service = new CampaignExpirationService(campaignRepository, Clock.fixed(now, ZoneOffset.UTC));

        assertThat(service.expireOverdueCampaigns()).isZero();
        verify(campaignRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
