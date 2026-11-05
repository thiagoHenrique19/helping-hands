package com.helpinghands.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.AbstractPostgresIntegrationTest;
import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.OrganizationRepository;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/** Writers that skip the pessimistic path (edits, closing, scheduler) must still never overwrite each other. */
class OptimisticLockingIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private CampaignRepository campaigns;
    @Autowired private VolunteerOpportunityRepository opportunities;
    @Autowired private OrganizationRepository organizations;

    private Organization organization() {
        return organizations.save(Organization.register("Org", UUID.randomUUID() + "@org.org", "d"));
    }

    @Test
    void shouldRejectStaleCampaignWrite() {
        Long id = campaigns.save(Campaign.create(organization(), "Stale", "d", new BigDecimal("100"))).getId();
        Campaign first = campaigns.findById(id).orElseThrow();
        Campaign second = campaigns.findById(id).orElseThrow();
        first.cancel();
        campaigns.save(first);

        second.update("Changed", "d", new BigDecimal("200"), null, null);

        assertThatThrownBy(() -> campaigns.save(second)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(campaigns.findById(id).orElseThrow().getTitle()).isEqualTo("Stale");
    }

    @Test
    void shouldRejectStaleOpportunityWrite() {
        Instant startsAt = Instant.now().plus(3, ChronoUnit.DAYS);
        Long id = opportunities.save(VolunteerOpportunity.create(organization(), "Task", "d", "Somewhere", false,
                startsAt, startsAt.plus(2, ChronoUnit.HOURS), 2)).getId();
        VolunteerOpportunity first = opportunities.findById(id).orElseThrow();
        VolunteerOpportunity second = opportunities.findById(id).orElseThrow();
        first.fillSlot();
        opportunities.save(first);

        second.close();

        assertThatThrownBy(() -> opportunities.save(second)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(opportunities.findById(id).orElseThrow().getFilledSlots()).isEqualTo(1);
    }
}
