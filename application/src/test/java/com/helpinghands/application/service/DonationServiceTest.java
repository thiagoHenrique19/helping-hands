package com.helpinghands.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpinghands.core.port.in.dto.DonationRequest;
import com.helpinghands.core.port.in.dto.DonationResponse;
import com.helpinghands.application.mapper.DonationMapper;
import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.domain.Donation;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.DonationRepository;
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

    private DonationService donationService;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        donationService = new DonationService(campaignRepository, donationRepository, new DonationMapper());
        Organization organization = Organization.register("Green Future", "hi@green.org", "Reforestation");
        campaign = Campaign.create(organization, "Plant 1000 trees", "Atlantic forest", new BigDecimal("100"));
    }

    @Test
    void shouldRegisterDonationAndUpdateCampaign() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DonationResponse response = donationService.donate(1L,
                new DonationRequest("Alice", "alice@mail.com", new BigDecimal("40")));

        assertThat(response.amount()).isEqualByComparingTo("40");
        assertThat(response.donorName()).isEqualTo("Alice");
        assertThat(campaign.getRaisedAmount()).isEqualByComparingTo("40");
    }

    @Test
    void shouldCompleteCampaignWhenGoalIsReached() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        donationService.donate(1L, new DonationRequest("Bob", "bob@mail.com", new BigDecimal("100")));

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
    }

    @Test
    void shouldFailWhenCampaignDoesNotExist() {
        when(campaignRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> donationService.donate(99L,
                new DonationRequest("Carol", "carol@mail.com", BigDecimal.TEN)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(donationRepository, never()).save(any());
    }
}
