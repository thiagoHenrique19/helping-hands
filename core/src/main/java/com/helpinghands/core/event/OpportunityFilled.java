package com.helpinghands.core.event;

import java.time.Instant;

public record OpportunityFilled(Long opportunityId, Long organizationId, Instant occurredAt) implements DomainEvent {

    public OpportunityFilled(Long opportunityId, Long organizationId) {
        this(opportunityId, organizationId, Instant.now());
    }
}
