package com.helpinghands.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpinghands.application.mapper.DonationMapper;
import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.domain.Donation;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.PaymentMethod;
import com.helpinghands.core.exception.BusinessRuleException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.port.in.dto.DonationRequest;
import com.helpinghands.core.port.in.dto.DonationResponse;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.DonationRepository;
import com.helpinghands.core.port.out.ReceiptCodeGenerator;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock private CampaignRepository campaignRepository;
    @Mock private DonationRepository donationRepository;
    @Mock private ReceiptCodeGenerator receiptCodeGenerator;

    private DonationService donationService;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        donationService = new DonationService(campaignRepository, donationRepository, receiptCodeGenerator,
                new DonationMapper());
        Organization organization = Organization.register("Green Future", "hi@green.org", "Reforestation");
        campaign = Campaign.create(organization, "Plant 1000 trees", "Atlantic forest", new BigDecimal("100"));
    }

    private DonationRequest request(String name, String amount) {
        return new DonationRequest(name, name.toLowerCase() + "@mail.com", new BigDecimal(amount), false, null,
                PaymentMethod.PIX);
    }

    @Test
    void shouldRegisterDonationAndUpdateCampaign() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(receiptCodeGenerator.next()).thenReturn("DON-00000007");
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DonationResponse response = donationService.donate(1L, new DonationRequest("Alice", "alice@mail.com",
                new BigDecimal("40"), true, "Keep going", PaymentMethod.CREDIT_CARD));

        assertThat(response.amount()).isEqualByComparingTo("40");
        assertThat(response.receiptCode()).isEqualTo("DON-00000007");
        assertThat(response.anonymous()).isTrue();
        assertThat(response.message()).isEqualTo("Keep going");
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(campaign.getRaisedAmount()).isEqualByComparingTo("40");
    }

    @Test
    void shouldCompleteCampaignWhenGoalIsReached() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(receiptCodeGenerator.next()).thenReturn("DON-1");
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        donationService.donate(1L, request("Bob", "100"));

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
    }

    @Test
    void shouldRejectDonationBelowMinimumAmount() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(receiptCodeGenerator.next()).thenReturn("DON-1");

        assertThatThrownBy(() -> donationService.donate(1L, request("Dan", "0.50")))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(campaign.getRaisedAmount()).isEqualByComparingTo("0");
        verify(donationRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenCampaignDoesNotExist() {
        when(campaignRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> donationService.donate(99L, request("Carol", "10")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(donationRepository, never()).save(any());
    }
}
