package com.helpinghands.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.helpinghands.AbstractPostgresIntegrationTest;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.domain.PaymentMethod;
import com.helpinghands.core.port.in.dto.analytics.CampaignProgressRank;
import com.helpinghands.core.port.in.dto.analytics.MonthlyDonations;
import com.helpinghands.core.port.in.dto.analytics.OrganizationDashboard;
import com.helpinghands.core.port.in.dto.analytics.PaymentMethodBreakdown;
import com.helpinghands.core.port.in.dto.analytics.PlatformSummary;
import com.helpinghands.core.port.in.dto.analytics.TopCampaign;
import com.helpinghands.core.port.out.ImpactAnalyticsRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ImpactAnalyticsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private ImpactAnalyticsRepository analytics;
    @Autowired private JdbcTemplate jdbc;

    private long orgA;
    private long orgB;
    private long campaignA1;
    private long campaignA2;
    private long campaignA3;
    private long campaignB1;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE organization, campaign, donation, volunteer_opportunity, volunteer_application CASCADE");
        orgA = insertOrganization("Org A", "a@org.org", true);
        orgB = insertOrganization("Org B", "b@org.org", false);
        campaignA1 = insertCampaign(orgA, "A1 completed", 100, 100, "COMPLETED");
        campaignA2 = insertCampaign(orgA, "A2 half", 200, 100, "ACTIVE");
        campaignA3 = insertCampaign(orgA, "A3 empty", 500, 0, "ACTIVE");
        campaignB1 = insertCampaign(orgB, "B1 quarter", 400, 100, "ACTIVE");

        insertDonation(campaignA1, "ann@mail.com", "100.00", "PIX", "2026-01-10T12:00:00Z");
        insertDonation(campaignA2, "ANN@mail.com", "40.00", "CREDIT_CARD", "2026-01-31T23:30:00Z");
        insertDonation(campaignA2, "bob@mail.com", "60.00", "PIX", "2026-03-05T08:00:00Z");
        insertDonation(campaignB1, "cid@mail.com", "100.00", "BOLETO", "2026-02-14T08:00:00Z");

        long openOpportunity = insertOpportunity(orgA, "OPEN");
        insertOpportunity(orgA, "CLOSED");
        insertOpportunity(orgB, "OPEN");
        insertApplication(openOpportunity, "v1@mail.com", "APPROVED");
        insertApplication(openOpportunity, "v2@mail.com", "APPROVED");
        insertApplication(openOpportunity, "v3@mail.com", "PENDING");
    }

    private long insertOrganization(String name, String email, boolean verified) {
        return jdbc.queryForObject("INSERT INTO organization (name, email, verified) VALUES (?, ?, ?) RETURNING id",
                Long.class, name, email, verified);
    }

    private long insertCampaign(long organizationId, String title, int goal, int raised, String status) {
        return jdbc.queryForObject("INSERT INTO campaign (organization_id, title, goal_amount, raised_amount, status) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, organizationId, title, goal, raised, status);
    }

    private void insertDonation(long campaignId, String email, String amount, String method, String createdAt) {
        jdbc.update("INSERT INTO donation (campaign_id, donor_name, donor_email, amount, created_at, payment_method, "
                        + "receipt_code) VALUES (?, 'Donor', ?, CAST(? AS numeric), CAST(? AS timestamptz), ?, "
                        + "'DON-' || lpad(nextval('donation_receipt_seq')::text, 8, '0'))",
                campaignId, email, amount, createdAt, method);
    }

    private long insertOpportunity(long organizationId, String status) {
        return jdbc.queryForObject("INSERT INTO volunteer_opportunity (organization_id, title, remote, starts_at, "
                + "ends_at, slots, status) VALUES (?, 'Task', true, now() + interval '1 day', now() + interval '2 day', "
                + "5, ?) RETURNING id", Long.class, organizationId, status);
    }

    private void insertApplication(long opportunityId, String email, String status) {
        jdbc.update("INSERT INTO volunteer_application (opportunity_id, volunteer_name, volunteer_email, status) "
                + "VALUES (?, 'Vol', ?, ?)", opportunityId, email, status);
    }

    @Test
    void shouldBuildOrganizationDashboard() {
        OrganizationDashboard dashboard = analytics.organizationDashboard(orgA);

        assertThat(dashboard.totalRaised()).isEqualByComparingTo("200.00");
        assertThat(dashboard.donationsCount()).isEqualTo(3);
        assertThat(dashboard.uniqueDonors()).isEqualTo(2);
        assertThat(dashboard.activeCampaigns()).isEqualTo(2);
        assertThat(dashboard.completedCampaigns()).isEqualTo(1);
        assertThat(dashboard.openOpportunities()).isEqualTo(1);
        assertThat(dashboard.approvedVolunteers()).isEqualTo(2);
    }

    @Test
    void shouldReturnZeroedDashboardForOrganizationWithoutActivity() {
        long empty = insertOrganization("Empty", "empty@org.org", false);

        OrganizationDashboard dashboard = analytics.organizationDashboard(empty);

        assertThat(dashboard.totalRaised()).isEqualByComparingTo("0");
        assertThat(dashboard.donationsCount()).isZero();
        assertThat(dashboard.approvedVolunteers()).isZero();
    }

    @Test
    void shouldBuildPlatformSummary() {
        PlatformSummary summary = analytics.platformSummary();

        assertThat(summary.organizations()).isEqualTo(2);
        assertThat(summary.verifiedOrganizations()).isEqualTo(1);
        assertThat(summary.campaigns()).isEqualTo(4);
        assertThat(summary.activeCampaigns()).isEqualTo(3);
        assertThat(summary.totalRaised()).isEqualByComparingTo("300.00");
        assertThat(summary.donationsCount()).isEqualTo(4);
        assertThat(summary.uniqueDonors()).isEqualTo(3);
        assertThat(summary.openOpportunities()).isEqualTo(2);
        assertThat(summary.approvedVolunteers()).isEqualTo(2);
    }

    @Test
    void shouldRankTopCampaignsByRaisedAmountPlatformWideAndPerOrganization() {
        List<TopCampaign> platform = analytics.topCampaigns(null, 2);
        List<TopCampaign> forOrgA = analytics.topCampaigns(orgA, 10);

        assertThat(platform).extracting(TopCampaign::title).containsExactly("A1 completed", "A2 half");
        assertThat(forOrgA).extracting(TopCampaign::title).containsExactly("A1 completed", "A2 half");
        assertThat(analytics.topCampaigns(orgB, 10)).extracting(TopCampaign::campaignId).containsExactly(campaignB1);
    }

    @Test
    void shouldGroupDonationsByMonthFillingEmptyMonths() {
        List<MonthlyDonations> months = analytics.donationsByMonth(orgA, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 30));

        assertThat(months).extracting(MonthlyDonations::month).containsExactly(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));
        assertThat(months).extracting(MonthlyDonations::donationsCount).containsExactly(2L, 0L, 1L, 0L);
        assertThat(months.get(0).totalAmount()).isEqualByComparingTo("140.00");
        assertThat(months.get(1).totalAmount()).isEqualByComparingTo("0");
        assertThat(months.get(2).totalAmount()).isEqualByComparingTo("60.00");
    }

    @Test
    void shouldRestrictDonationsToTheRequestedDates() {
        List<MonthlyDonations> months = analytics.donationsByMonth(orgA, LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 3, 1));

        assertThat(months).hasSize(3);
        assertThat(months.get(0).donationsCount()).isEqualTo(1);
        assertThat(months.get(0).totalAmount()).isEqualByComparingTo("40.00");
        assertThat(months.get(2).donationsCount()).isZero();
    }

    @Test
    void shouldRankCampaignProgressWithWindowFunctionHandlingTies() {
        insertCampaign(orgA, "A4 also complete", 50, 50, "COMPLETED");

        List<CampaignProgressRank> ranking = analytics.campaignProgressRanking(orgA);

        assertThat(ranking).extracting(CampaignProgressRank::title)
                .containsExactly("A1 completed", "A4 also complete", "A2 half", "A3 empty");
        assertThat(ranking).extracting(CampaignProgressRank::rank).containsExactly(1, 1, 3, 4);
        assertThat(ranking.get(2).progressPercentage()).isEqualByComparingTo("50.00");
        assertThat(ranking.get(0).status()).isEqualTo(CampaignStatus.COMPLETED);
        assertThat(ranking).extracting(CampaignProgressRank::campaignId).doesNotContain(campaignB1);
        assertThat(campaignA3).isPositive();
    }

    @Test
    void shouldBreakDownDonationsByPaymentMethod() {
        List<PaymentMethodBreakdown> platform = analytics.donationsByPaymentMethod(null);
        List<PaymentMethodBreakdown> forOrgA = analytics.donationsByPaymentMethod(orgA);

        assertThat(platform).extracting(PaymentMethodBreakdown::paymentMethod)
                .containsExactly(PaymentMethod.PIX, PaymentMethod.BOLETO, PaymentMethod.CREDIT_CARD);
        assertThat(platform.get(0).totalAmount()).isEqualByComparingTo("160.00");
        assertThat(platform.get(0).donationsCount()).isEqualTo(2);
        assertThat(forOrgA).extracting(PaymentMethodBreakdown::paymentMethod)
                .containsExactly(PaymentMethod.PIX, PaymentMethod.CREDIT_CARD);
    }
}
