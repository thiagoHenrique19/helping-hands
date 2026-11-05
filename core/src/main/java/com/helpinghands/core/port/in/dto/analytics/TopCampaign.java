package com.helpinghands.core.port.in.dto.analytics;

import java.math.BigDecimal;

public record TopCampaign(Long campaignId, Long organizationId, String title, BigDecimal raisedAmount,
                          BigDecimal goalAmount) {
}
