package com.helpinghands.infrastructure.event;

import com.helpinghands.core.event.CampaignCompleted;
import com.helpinghands.core.event.OpportunityFilled;
import com.helpinghands.core.event.VolunteerApplicationApproved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class DomainEventLoggingListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventLoggingListener.class);

    @TransactionalEventListener(fallbackExecution = true)
    void on(CampaignCompleted event) {
        log.info("Campaign {} of organization {} completed with {} raised",
                event.campaignId(), event.organizationId(), event.raisedAmount());
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(VolunteerApplicationApproved event) {
        log.info("Volunteer application {} approved for opportunity {} ({})",
                event.applicationId(), event.opportunityId(), event.volunteerEmail());
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(OpportunityFilled event) {
        log.info("Opportunity {} of organization {} is now filled", event.opportunityId(), event.organizationId());
    }
}
