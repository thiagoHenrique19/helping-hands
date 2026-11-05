package com.helpinghands.core.event;

import java.math.BigDecimal;
import java.time.Instant;

public record CampaignCompleted(Long campaignId, Long organizationId, BigDecimal raisedAmount, Instant occurredAt)
        implements DomainEvent {

    public CampaignCompleted(Long campaignId, Long organizationId, BigDecimal raisedAmount) {
        this(campaignId, organizationId, raisedAmount, Instant.now());
    }
}
