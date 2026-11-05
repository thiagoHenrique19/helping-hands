package com.helpinghands.core.event;

import java.time.Instant;

public record VolunteerApplicationApproved(Long applicationId, Long opportunityId, String volunteerEmail,
                                           Instant occurredAt) implements DomainEvent {

    public VolunteerApplicationApproved(Long applicationId, Long opportunityId, String volunteerEmail) {
        this(applicationId, opportunityId, volunteerEmail, Instant.now());
    }
}
