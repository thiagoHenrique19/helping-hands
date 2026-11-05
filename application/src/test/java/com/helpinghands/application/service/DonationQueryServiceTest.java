package com.helpinghands.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpinghands.application.mapper.DonationMapper;
import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.Donation;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.PaymentMethod;
import com.helpinghands.core.exception.InvalidRequestException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.PublicDonationResponse;
import com.helpinghands.core.port.in.dto.TopDonorResponse;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.DonationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DonationQueryServiceTest {

    @Mock private CampaignRepository campaignRepository;
    @Mock private DonationRepository donationRepository;

    private DonationQueryService service;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        service = new DonationQueryService(campaignRepository, donationRepository, new DonationMapper());
        campaign = Campaign.create(Organization.register("Org", "o@org.org", "d"), "T", "d", new BigDecimal("500"));
    }

    private Donation donation(String name, boolean anonymous) {
        return Donation.make(campaign, name, name + "@mail.com", BigDecimal.TEN, anonymous, "hi",
                PaymentMethod.PIX, "DON-1");
    }

    @Test
    void shouldMaskAnonymousDonorsInPublicListing() {
        var query = PageQuery.of(0, 10);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(donationRepository.findByCampaignId(1L, query)).thenReturn(
                new PageResult<>(List.of(donation("alice", false), donation("bob", true)), 0, 10, 2, 1));

        PageResult<PublicDonationResponse> result = service.listByCampaign(1L, query);

        assertThat(result.content()).extracting(PublicDonationResponse::donorName)
                .containsExactly("alice", "Anonymous");
    }

    @Test
    void shouldFailListingForUnknownCampaign() {
        when(campaignRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByCampaign(9L, PageQuery.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnTopDonors() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        var top = List.of(new TopDonorResponse("alice", new BigDecimal("30"), 2));
        when(donationRepository.findTopDonors(1L, 5)).thenReturn(top);

        assertThat(service.topDonors(1L, 5)).isEqualTo(top);
        verify(donationRepository).findTopDonors(1L, 5);
    }

    @Test
    void shouldRejectInvalidTopDonorsLimit() {
        assertThatThrownBy(() -> service.topDonors(1L, 0)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.topDonors(1L, 51)).isInstanceOf(InvalidRequestException.class);
    }
}
