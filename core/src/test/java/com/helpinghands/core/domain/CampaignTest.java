package com.helpinghands.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.core.exception.BusinessRuleException;
import java.math.BigDecimal;
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
}
