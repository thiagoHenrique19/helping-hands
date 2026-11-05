package com.helpinghands.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.core.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DonationTest {

    private final Organization organization = Organization.register("Org", "o@org.org", "d");
    private final Campaign campaign = Campaign.create(organization, "Title", "d", new BigDecimal("1000"));

    private Donation donation(String amount, boolean anonymous) {
        return Donation.make(campaign, "Alice", "alice@mail.com", new BigDecimal(amount), anonymous,
                "Good luck!", PaymentMethod.PIX, "DON-00000001");
    }

    @Test
    void shouldKeepDonationDetails() {
        Donation donation = donation("25.00", false);

        assertThat(donation.getMessage()).isEqualTo("Good luck!");
        assertThat(donation.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
        assertThat(donation.getReceiptCode()).isEqualTo("DON-00000001");
        assertThat(donation.isAnonymous()).isFalse();
    }

    @Test
    void shouldExposeDonorNameForPublicDonation() {
        assertThat(donation("25.00", false).publicDonorName()).isEqualTo("Alice");
    }

    @Test
    void shouldMaskDonorNameForAnonymousDonation() {
        assertThat(donation("25.00", true).publicDonorName()).isEqualTo(Donation.ANONYMOUS_DONOR_NAME);
    }

    @Test
    void shouldAcceptDonationEqualToMinimumAmount() {
        assertThat(donation("1.00", false).getAmount()).isEqualByComparingTo("1.00");
    }

    @Test
    void shouldRejectDonationBelowMinimumAmount() {
        assertThatThrownBy(() -> donation("0.99", false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("minimum");
    }

    @Test
    void shouldRequirePaymentMethod() {
        assertThatThrownBy(() -> Donation.make(campaign, "Alice", "alice@mail.com", BigDecimal.TEN,
                false, null, null, "DON-1"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
