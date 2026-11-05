package com.helpinghands.core.port.in.dto.analytics;

import com.helpinghands.core.domain.CampaignStatus;
import java.math.BigDecimal;

public record CampaignProgressRank(int rank, Long campaignId, String title, CampaignStatus status,
                                   BigDecimal raisedAmount, BigDecimal goalAmount, BigDecimal progressPercentage) {
}
