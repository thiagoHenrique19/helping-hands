package com.helpinghands;

import static org.assertj.core.api.Assertions.assertThat;

import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.event.OpportunityFilled;
import com.helpinghands.core.event.VolunteerApplicationApproved;
import com.helpinghands.core.exception.BusinessRuleException;
import com.helpinghands.core.port.in.ManageApplicationsUseCase;
import com.helpinghands.core.port.in.dto.ApplicationRequest;
import com.helpinghands.core.port.out.OrganizationRepository;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(RecordedEvents.class)
class ConcurrentApprovalsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private ManageApplicationsUseCase applications;
    @Autowired private VolunteerOpportunityRepository opportunities;
    @Autowired private OrganizationRepository organizations;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RecordedEvents events;

    private Long newOpportunity(int slots) {
        var organization = organizations.save(Organization.register("Org", UUID.randomUUID() + "@org.org", "d"));
        Instant startsAt = Instant.now().plus(3, ChronoUnit.DAYS);
        return opportunities.save(VolunteerOpportunity.create(organization, "Task", "d", "Somewhere", false,
                startsAt, startsAt.plus(2, ChronoUnit.HOURS), slots)).getId();
    }

    private List<Long> pendingApplications(Long opportunityId, int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(applications.apply(opportunityId,
                    new ApplicationRequest("Vol " + i, "vol" + i + "@mail.com", null, null), null).id());
        }
        return ids;
    }

    private List<ConcurrentRunner.Outcome<Object>> approveAll(List<Long> applicationIds) throws Exception {
        List<Callable<Object>> tasks = new ArrayList<>();
        applicationIds.forEach(id -> tasks.add(() -> applications.approve(id)));
        return ConcurrentRunner.runAll(tasks);
    }

    @Test
    void shouldLetExactlyOneVolunteerTakeTheLastSlot() throws Exception {
        Long opportunityId = newOpportunity(1);

        var outcomes = approveAll(pendingApplications(opportunityId, 10));

        assertThat(outcomes).filteredOn(ConcurrentRunner.Outcome::succeeded).hasSize(1);
        assertThat(outcomes).filteredOn(o -> !o.succeeded())
                .hasSize(9)
                .allMatch(o -> o.error() instanceof BusinessRuleException, "losers get a clean business error");
        assertThat(jdbc.queryForObject("SELECT filled_slots FROM volunteer_opportunity WHERE id = ?",
                Integer.class, opportunityId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM volunteer_opportunity WHERE id = ?",
                String.class, opportunityId)).isEqualTo("FULL");
        assertThat(events.of(OpportunityFilled.class)).filteredOn(e -> e.opportunityId().equals(opportunityId))
                .hasSize(1);
    }

    @Test
    void shouldNeverApproveMoreVolunteersThanSlots() throws Exception {
        Long opportunityId = newOpportunity(3);

        var outcomes = approveAll(pendingApplications(opportunityId, 12));

        assertThat(outcomes).filteredOn(ConcurrentRunner.Outcome::succeeded).hasSize(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM volunteer_application WHERE opportunity_id = ? "
                + "AND status = 'APPROVED'", Integer.class, opportunityId)).isEqualTo(3);
        assertThat(events.of(VolunteerApplicationApproved.class))
                .filteredOn(e -> e.opportunityId().equals(opportunityId)).hasSize(3);
    }
}
