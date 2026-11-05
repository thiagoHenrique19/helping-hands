package com.helpinghands.core.port.in.dto;

import com.helpinghands.core.domain.CampaignCategory;
import com.helpinghands.core.domain.CampaignStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record CampaignResponse(
        Long id,
        Long organizationId,
        String title,
        String description,
        BigDecimal goalAmount,
        BigDecimal raisedAmount,
        BigDecimal progressPercentage,
        CampaignStatus status,
        CampaignCategory category,
        Instant endsAt) {
}
