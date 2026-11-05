package com.helpinghands.core.port.in.dto;

import com.helpinghands.core.domain.CampaignStatus;
import java.math.BigDecimal;

public record CampaignResponse(
        Long id,
        Long organizationId,
        String title,
        String description,
        BigDecimal goalAmount,
        BigDecimal raisedAmount,
        BigDecimal progressPercentage,
        CampaignStatus status) {
}
