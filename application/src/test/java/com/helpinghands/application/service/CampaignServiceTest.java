package com.helpinghands.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.helpinghands.core.port.in.dto.CampaignRequest;
import com.helpinghands.core.port.in.dto.CampaignResponse;
import com.helpinghands.application.mapper.CampaignMapper;
import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.OrganizationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock private CampaignRepository campaignRepository;
    @Mock private OrganizationRepository organizationRepository;

    private CampaignService campaignService;
    private final Organization organization = Organization.register("Books For All", "b@books.org", "Libraries");

    @BeforeEach
    void setUp() {
        campaignService = new CampaignService(campaignRepository, organizationRepository, new CampaignMapper());
    }

    @Test
    void shouldCreateCampaignForExistingOrganization() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CampaignResponse response = campaignService.create(1L,
                new CampaignRequest("New library", "Build it", new BigDecimal("5000")));

        assertThat(response.title()).isEqualTo("New library");
        assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    void shouldFailWhenOrganizationDoesNotExist() {
        when(organizationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.create(5L,
                new CampaignRequest("X", "Y", BigDecimal.TEN)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldListActiveCampaigns() {
        Campaign campaign = Campaign.create(organization, "Active one", "desc", BigDecimal.TEN);
        when(campaignRepository.findByStatus(CampaignStatus.ACTIVE)).thenReturn(List.of(campaign));

        assertThat(campaignService.listActive()).extracting(CampaignResponse::title).containsExactly("Active one");
    }
}
