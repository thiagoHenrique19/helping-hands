package com.helpinghands.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.AbstractPostgresIntegrationTest;
import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignCategory;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.exception.InvalidRequestException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.pagination.SortDirection;
import com.helpinghands.core.port.in.dto.CampaignSearchCriteria;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.OrganizationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class CampaignPersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private CampaignRepository campaignRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Organization organization;
    private Organization otherOrganization;

    @BeforeEach
    void setUp() {
        organization = organizationRepository.save(
                Organization.register("Org " + UUID.randomUUID(), UUID.randomUUID() + "@org.org", "d"));
        otherOrganization = organizationRepository.save(
                Organization.register("Other " + UUID.randomUUID(), UUID.randomUUID() + "@org.org", "d"));
    }

    private Campaign campaign(Organization owner, String title, String goal, CampaignCategory category) {
        return campaignRepository.save(Campaign.create(owner, title, "d", new BigDecimal(goal), null, category));
    }

    @Test
    void shouldFilterByOrganizationStatusCategoryAndTitle() {
        campaign(organization, "School books", "100", CampaignCategory.EDUCATION);
        campaign(organization, "Vaccines", "100", CampaignCategory.HEALTH);
        Campaign cancelled = campaign(organization, "School roof", "100", CampaignCategory.EDUCATION);
        cancelled.cancel();
        campaignRepository.save(cancelled);
        campaign(otherOrganization, "School lunch", "100", CampaignCategory.EDUCATION);

        var criteria = new CampaignSearchCriteria(organization.getId(), CampaignStatus.ACTIVE,
                CampaignCategory.EDUCATION, "SCHOOL");
        PageResult<Campaign> result = campaignRepository.search(criteria, PageQuery.of(0, 10));

        assertThat(result.content()).extracting(Campaign::getTitle).containsExactly("School books");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldTreatLikeWildcardsAsLiteralText() {
        campaign(organization, "100% funded", "100", CampaignCategory.OTHER);
        campaign(organization, "Plain title", "100", CampaignCategory.OTHER);

        var criteria = new CampaignSearchCriteria(organization.getId(), null, null, "%");

        assertThat(campaignRepository.search(criteria, PageQuery.of(0, 10)).content())
                .extracting(Campaign::getTitle).containsExactly("100% funded");
    }

    @Test
    void shouldPaginateAndSort() {
        campaign(organization, "B", "300", CampaignCategory.OTHER);
        campaign(organization, "A", "100", CampaignCategory.OTHER);
        campaign(organization, "C", "200", CampaignCategory.OTHER);
        var criteria = new CampaignSearchCriteria(organization.getId(), null, null, null);

        PageResult<Campaign> first = campaignRepository.search(criteria,
                new PageQuery(0, 2, "goalAmount", SortDirection.DESC));
        PageResult<Campaign> second = campaignRepository.search(criteria,
                new PageQuery(1, 2, "goalAmount", SortDirection.DESC));

        assertThat(first.content()).extracting(Campaign::getTitle).containsExactly("B", "C");
        assertThat(second.content()).extracting(Campaign::getTitle).containsExactly("A");
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldRejectUnknownSortField() {
        assertThatThrownBy(() -> campaignRepository.search(CampaignSearchCriteria.none(),
                new PageQuery(0, 5, "password", SortDirection.ASC)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shouldFindOnlyActiveCampaignsWithPassedDeadline() {
        Campaign overdue = campaignRepository.save(Campaign.create(organization, "Overdue", "d",
                BigDecimal.TEN, Instant.now().plus(1, ChronoUnit.DAYS), CampaignCategory.OTHER));
        Campaign upcoming = campaignRepository.save(Campaign.create(organization, "Upcoming", "d",
                BigDecimal.TEN, Instant.now().plus(5, ChronoUnit.DAYS), CampaignCategory.OTHER));
        Campaign withoutDeadline = campaign(organization, "Open ended", "10", CampaignCategory.OTHER);
        jdbcTemplate.update("UPDATE campaign SET ends_at = now() - interval '1 hour' WHERE id = ?", overdue.getId());

        var found = campaignRepository.findActiveWithDeadlineUpTo(Instant.now());

        assertThat(found).extracting(Campaign::getId).contains(overdue.getId())
                .doesNotContain(upcoming.getId(), withoutDeadline.getId());
    }
}
