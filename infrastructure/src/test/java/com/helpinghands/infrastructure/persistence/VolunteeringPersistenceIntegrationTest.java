package com.helpinghands.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.AbstractPostgresIntegrationTest;
import com.helpinghands.core.domain.ApplicationStatus;
import com.helpinghands.core.domain.OpportunityStatus;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.VolunteerApplication;
import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.port.in.dto.OpportunitySearchCriteria;
import com.helpinghands.core.port.out.OrganizationRepository;
import com.helpinghands.core.port.out.VolunteerApplicationRepository;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class VolunteeringPersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private VolunteerOpportunityRepository opportunityRepository;
    @Autowired private VolunteerApplicationRepository applicationRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = organizationRepository.save(Organization.register("Org", UUID.randomUUID() + "@org.org", "d"));
    }

    private VolunteerOpportunity opportunity(boolean remote, int slots) {
        Instant startsAt = Instant.now().plus(3, ChronoUnit.DAYS);
        return opportunityRepository.save(VolunteerOpportunity.create(organization, "Task", "d",
                remote ? null : "Somewhere", remote, startsAt, startsAt.plus(2, ChronoUnit.HOURS), slots));
    }

    @Test
    void shouldFilterOpportunitiesByRemoteStatusAndUpcoming() {
        VolunteerOpportunity remote = opportunity(true, 2);
        VolunteerOpportunity onSite = opportunity(false, 2);
        VolunteerOpportunity past = opportunity(true, 2);
        jdbcTemplate.update("UPDATE volunteer_opportunity SET starts_at = now() - interval '2 days', "
                + "ends_at = now() - interval '1 day' WHERE id = ?", past.getId());
        VolunteerOpportunity closed = opportunity(true, 2);
        closed.close();
        opportunityRepository.save(closed);

        var criteria = new OpportunitySearchCriteria(organization.getId(), OpportunityStatus.OPEN, true, true);
        var result = opportunityRepository.search(criteria, Instant.now(), PageQuery.of(0, 10));

        assertThat(result.content()).extracting(VolunteerOpportunity::getId).containsExactly(remote.getId());

        var all = opportunityRepository.search(new OpportunitySearchCriteria(organization.getId(), null, null, null),
                Instant.now(), PageQuery.of(0, 10));
        assertThat(all.totalElements()).isEqualTo(4);
        assertThat(all.content()).extracting(VolunteerOpportunity::getId).contains(onSite.getId());
    }

    @Test
    void shouldPersistSlotChangesAndAdvanceVersion() {
        VolunteerOpportunity opportunity = opportunity(true, 1);
        VolunteerApplication application = applicationRepository.save(
                VolunteerApplication.apply(opportunity, "Vera", "vera@mail.com", null, null));

        application.approve();
        opportunityRepository.save(opportunity);
        applicationRepository.save(application);

        VolunteerOpportunity reloaded = opportunityRepository.findById(opportunity.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OpportunityStatus.FULL);
        assertThat(reloaded.getFilledSlots()).isEqualTo(1);
    }

    @Test
    void shouldEnforceUniqueApplicationPerOpportunityAndEmail() {
        VolunteerOpportunity opportunity = opportunity(true, 3);
        applicationRepository.save(VolunteerApplication.apply(opportunity, "Vera", "vera@mail.com", null, null));

        assertThat(applicationRepository.existsByOpportunityIdAndVolunteerEmail(opportunity.getId(), "vera@mail.com"))
                .isTrue();
        assertThatThrownBy(() -> applicationRepository.save(
                VolunteerApplication.apply(opportunity, "Vera 2", "vera@mail.com", null, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldListApplicationsFilteredByStatus() {
        VolunteerOpportunity opportunity = opportunity(true, 3);
        VolunteerApplication pending = applicationRepository.save(
                VolunteerApplication.apply(opportunity, "A", "a@mail.com", null, null));
        VolunteerApplication rejected = VolunteerApplication.apply(opportunity, "B", "b@mail.com", null, null);
        rejected.reject();
        applicationRepository.save(rejected);

        var onlyPending = applicationRepository.findByOpportunityId(opportunity.getId(), ApplicationStatus.PENDING,
                PageQuery.of(0, 10));
        var all = applicationRepository.findByOpportunityId(opportunity.getId(), null, PageQuery.of(0, 10));

        assertThat(onlyPending.content()).extracting(VolunteerApplication::getId).containsExactly(pending.getId());
        assertThat(all.totalElements()).isEqualTo(2);
    }
}
