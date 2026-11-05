package com.helpinghands.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.core.exception.BusinessRuleException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class VolunteerApplicationTest {

    private final Organization organization = Organization.register("Org", "o@org.org", "d");

    private VolunteerOpportunity opportunity(int slots) {
        Instant startsAt = Instant.now().plus(5, ChronoUnit.DAYS);
        return VolunteerOpportunity.create(organization, "Clean-up", "d", "Beach", false,
                startsAt, startsAt.plus(3, ChronoUnit.HOURS), slots);
    }

    private VolunteerApplication apply(VolunteerOpportunity opportunity, String email) {
        return VolunteerApplication.apply(opportunity, "Vera", email, "1199", "I care");
    }

    @Test
    void shouldStartPendingWithNormalizedEmail() {
        VolunteerApplication application = apply(opportunity(2), " Vera@Mail.com ");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(application.getVolunteerEmail()).isEqualTo("vera@mail.com");
    }

    @Test
    void shouldRejectApplyingToClosedOpportunity() {
        VolunteerOpportunity opportunity = opportunity(2);
        opportunity.close();

        assertThatThrownBy(() -> apply(opportunity, "a@mail.com")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldApproveAndFillSlot() {
        VolunteerOpportunity opportunity = opportunity(2);
        VolunteerApplication application = apply(opportunity, "a@mail.com");

        application.approve();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(opportunity.getFilledSlots()).isEqualTo(1);
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.OPEN);
    }

    @Test
    void shouldMarkOpportunityFullWhenLastSlotIsApproved() {
        VolunteerOpportunity opportunity = opportunity(1);

        apply(opportunity, "a@mail.com").approve();

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.FULL);
    }

    @Test
    void shouldNotApproveBeyondSlots() {
        VolunteerOpportunity opportunity = opportunity(1);
        apply(opportunity, "a@mail.com").approve();
        VolunteerApplication second = apply(opportunity, "b@mail.com");

        assertThatThrownBy(second::approve).isInstanceOf(BusinessRuleException.class);
        assertThat(second.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void shouldOnlyApprovePendingApplications() {
        VolunteerApplication application = apply(opportunity(3), "a@mail.com");
        application.approve();

        assertThatThrownBy(application::approve).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldRejectPendingApplication() {
        VolunteerOpportunity opportunity = opportunity(1);
        VolunteerApplication application = apply(opportunity, "a@mail.com");

        application.reject();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(opportunity.getFilledSlots()).isZero();
        assertThatThrownBy(application::reject).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(application::approve).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldWithdrawPendingApplicationWithoutTouchingSlots() {
        VolunteerOpportunity opportunity = opportunity(1);
        VolunteerApplication application = apply(opportunity, "a@mail.com");

        application.withdraw();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(opportunity.getFilledSlots()).isZero();
    }

    @Test
    void shouldFreeSlotAndReopenWhenApprovedApplicationIsWithdrawn() {
        VolunteerOpportunity opportunity = opportunity(1);
        VolunteerApplication application = apply(opportunity, "a@mail.com");
        application.approve();
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.FULL);

        application.withdraw();

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.OPEN);
        assertThat(opportunity.getFilledSlots()).isZero();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void shouldNotWithdrawRejectedOrWithdrawnApplications() {
        VolunteerApplication rejected = apply(opportunity(1), "a@mail.com");
        rejected.reject();
        VolunteerApplication withdrawn = apply(opportunity(1), "b@mail.com");
        withdrawn.withdraw();

        assertThatThrownBy(rejected::withdraw).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(withdrawn::withdraw).isInstanceOf(BusinessRuleException.class);
    }
}
