package com.helpinghands.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.core.exception.BusinessRuleException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class VolunteerOpportunityTest {

    private final Organization organization = Organization.register("Org", "o@org.org", "d");
    private final Instant startsAt = Instant.now().plus(10, ChronoUnit.DAYS);
    private final Instant endsAt = startsAt.plus(4, ChronoUnit.HOURS);

    private VolunteerOpportunity opportunity(int slots) {
        return VolunteerOpportunity.create(organization, "Food drive", "Sort donations", "Sao Paulo", false,
                startsAt, endsAt, slots);
    }

    @Test
    void shouldStartOpenWithNoFilledSlots() {
        VolunteerOpportunity opportunity = opportunity(3);

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.OPEN);
        assertThat(opportunity.getFilledSlots()).isZero();
        assertThat(opportunity.availableSlots()).isEqualTo(3);
    }

    @Test
    void shouldRejectInvalidSlots() {
        assertThatThrownBy(() -> opportunity(0)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldRejectEndBeforeStart() {
        assertThatThrownBy(() -> VolunteerOpportunity.create(organization, "T", "d", "L", false,
                startsAt, startsAt.minusSeconds(1), 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("after");
    }

    @Test
    void shouldRejectStartInThePast() {
        assertThatThrownBy(() -> VolunteerOpportunity.create(organization, "T", "d", "L", false,
                Instant.now().minusSeconds(60), endsAt, 1))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldRequireLocationWhenNotRemote() {
        assertThatThrownBy(() -> VolunteerOpportunity.create(organization, "T", "d", " ", false, startsAt, endsAt, 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Location");
        assertThat(VolunteerOpportunity.create(organization, "T", "d", null, true, startsAt, endsAt, 1).isRemote())
                .isTrue();
    }

    @Test
    void shouldBecomeFullWhenLastSlotIsFilled() {
        VolunteerOpportunity opportunity = opportunity(2);

        opportunity.fillSlot();
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.OPEN);
        opportunity.fillSlot();

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.FULL);
        assertThat(opportunity.availableSlots()).isZero();
    }

    @Test
    void shouldNotFillBeyondSlots() {
        VolunteerOpportunity opportunity = opportunity(1);
        opportunity.fillSlot();

        assertThatThrownBy(opportunity::fillSlot)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("slots");
    }

    @Test
    void shouldReopenWhenSlotIsReleasedFromFullOpportunity() {
        VolunteerOpportunity opportunity = opportunity(1);
        opportunity.fillSlot();

        opportunity.releaseSlot();

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.OPEN);
        assertThat(opportunity.getFilledSlots()).isZero();
    }

    @Test
    void shouldKeepClosedStatusWhenSlotIsReleased() {
        VolunteerOpportunity opportunity = opportunity(2);
        opportunity.fillSlot();
        opportunity.close();

        opportunity.releaseSlot();

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.CLOSED);
        assertThat(opportunity.getFilledSlots()).isZero();
    }

    @Test
    void shouldAcceptApplicationsWhenOpenOrFull() {
        VolunteerOpportunity opportunity = opportunity(1);
        opportunity.assertAcceptsApplications();
        opportunity.fillSlot();

        opportunity.assertAcceptsApplications();
    }

    @Test
    void shouldRejectApplicationsWhenClosedOrCancelled() {
        VolunteerOpportunity closed = opportunity(1);
        closed.close();
        VolunteerOpportunity cancelled = opportunity(1);
        cancelled.cancel();

        assertThatThrownBy(closed::assertAcceptsApplications).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(cancelled::assertAcceptsApplications).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldNotFillSlotsOfClosedOpportunity() {
        VolunteerOpportunity opportunity = opportunity(2);
        opportunity.close();

        assertThatThrownBy(opportunity::fillSlot).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldCloseOnlyOpenOrFullOpportunities() {
        VolunteerOpportunity opportunity = opportunity(2);
        opportunity.close();

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.CLOSED);
        assertThatThrownBy(opportunity::close).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldCancelUnlessAlreadyCancelled() {
        VolunteerOpportunity opportunity = opportunity(2);
        opportunity.close();
        opportunity.cancel();

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.CANCELLED);
        assertThatThrownBy(opportunity::cancel).isInstanceOf(BusinessRuleException.class);
    }
}
