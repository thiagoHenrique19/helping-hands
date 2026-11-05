package com.helpinghands.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.core.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CampaignTest {

    private final Organization organization = Organization.register("Paws Together", "contact@paws.org", "Animal rescue");

    private Campaign campaignWithGoal(String goal) {
        return Campaign.create(organization, "Shelter roof", "Fix the roof", new BigDecimal(goal));
    }

    @Test
    @DisplayName("new campaign starts active with nothing raised")
    void shouldStartActiveWithZeroRaised() {
        Campaign campaign = campaignWithGoal("1000");

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(campaign.getRaisedAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("donation increases raised amount")
    void shouldAccumulateDonations() {
        Campaign campaign = campaignWithGoal("1000");

        campaign.receiveDonation(new BigDecimal("250"));
        campaign.receiveDonation(new BigDecimal("100"));

        assertThat(campaign.getRaisedAmount()).isEqualByComparingTo("350");
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    @DisplayName("campaign completes when goal is reached")
    void shouldCompleteWhenGoalReached() {
        Campaign campaign = campaignWithGoal("500");

        campaign.receiveDonation(new BigDecimal("500"));

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
    }

    @Test
    @DisplayName("rejects non positive donation")
    void shouldRejectNonPositiveDonation() {
        Campaign campaign = campaignWithGoal("500");

        assertThatThrownBy(() -> campaign.receiveDonation(BigDecimal.ZERO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("rejects donation to cancelled campaign")
    void shouldRejectDonationWhenNotActive() {
        Campaign campaign = campaignWithGoal("500");
        campaign.cancel();

        assertThatThrownBy(() -> campaign.receiveDonation(BigDecimal.TEN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("progress percentage is capped at 100")
    void shouldCalculateProgress() {
        Campaign campaign = campaignWithGoal("200");

        campaign.receiveDonation(new BigDecimal("50"));

        assertThat(campaign.progressPercentage()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("rejects non positive goal")
    void shouldRejectInvalidGoal() {
        assertThatThrownBy(() -> campaignWithGoal("0")).isInstanceOf(BusinessRuleException.class);
    }

    private Campaign campaignEndingAt(Instant endsAt) {
        return Campaign.create(organization, "Shelter roof", "Fix the roof", new BigDecimal("1000"),
                endsAt, CampaignCategory.ANIMALS);
    }

    @Test
    @DisplayName("keeps optional deadline and category")
    void shouldKeepDeadlineAndCategory() {
        Instant endsAt = Instant.now().plus(10, ChronoUnit.DAYS);

        Campaign campaign = campaignEndingAt(endsAt);

        assertThat(campaign.getEndsAt()).isEqualTo(endsAt);
        assertThat(campaign.getCategory()).isEqualTo(CampaignCategory.ANIMALS);
        assertThat(campaignWithGoal("10").getCategory()).isEqualTo(CampaignCategory.OTHER);
        assertThat(campaignWithGoal("10").getEndsAt()).isNull();
    }

    @Test
    @DisplayName("rejects deadline in the past")
    void shouldRejectPastDeadline() {
        assertThatThrownBy(() -> campaignEndingAt(Instant.now().minus(1, ChronoUnit.DAYS)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");
    }

    @Test
    @DisplayName("updates active campaign")
    void shouldUpdateActiveCampaign() {
        Campaign campaign = campaignWithGoal("1000");
        campaign.receiveDonation(new BigDecimal("100"));

        campaign.update("New title", "New desc", new BigDecimal("2000"), null, CampaignCategory.HEALTH);

        assertThat(campaign.getTitle()).isEqualTo("New title");
        assertThat(campaign.getGoalAmount()).isEqualByComparingTo("2000");
        assertThat(campaign.getCategory()).isEqualTo(CampaignCategory.HEALTH);
        assertThat(campaign.getRaisedAmount()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("rejects goal not greater than raised amount")
    void shouldRejectGoalBelowRaised() {
        Campaign campaign = campaignWithGoal("1000");
        campaign.receiveDonation(new BigDecimal("600"));

        assertThatThrownBy(() -> campaign.update("T", "D", new BigDecimal("500"), null, CampaignCategory.OTHER))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("raised");
    }

    @Test
    @DisplayName("rejects update when campaign is not active")
    void shouldRejectUpdateWhenNotActive() {
        Campaign campaign = campaignWithGoal("1000");
        campaign.cancel();

        assertThatThrownBy(() -> campaign.update("T", "D", BigDecimal.TEN, null, CampaignCategory.OTHER))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active");
    }

    @Test
    @DisplayName("expires only when deadline has passed")
    void shouldExpireWhenDeadlinePassed() {
        Instant endsAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Campaign campaign = campaignEndingAt(endsAt);

        assertThat(campaign.expireIfOverdue(Instant.now())).isFalse();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);

        assertThat(campaign.expireIfOverdue(endsAt.plusSeconds(1))).isTrue();
        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.EXPIRED);
    }

    @Test
    @DisplayName("campaign without deadline never expires")
    void shouldNeverExpireWithoutDeadline() {
        Campaign campaign = campaignWithGoal("100");

        assertThat(campaign.expireIfOverdue(Instant.now().plus(3650, ChronoUnit.DAYS))).isFalse();
    }

    @Test
    @DisplayName("rejects donation to expired campaign")
    void shouldRejectDonationToExpiredCampaign() {
        Campaign campaign = campaignEndingAt(Instant.now().plus(1, ChronoUnit.DAYS));
        campaign.expireIfOverdue(Instant.now().plus(2, ChronoUnit.DAYS));

        assertThatThrownBy(() -> campaign.receiveDonation(BigDecimal.TEN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("only active campaigns can be cancelled")
    void shouldOnlyCancelActiveCampaigns() {
        Campaign campaign = campaignWithGoal("100");
        campaign.cancel();

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
        assertThatThrownBy(campaign::cancel).isInstanceOf(BusinessRuleException.class);
    }
}
