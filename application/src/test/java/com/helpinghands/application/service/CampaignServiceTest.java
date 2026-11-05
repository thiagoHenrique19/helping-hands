package com.helpinghands.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.helpinghands.core.port.in.dto.CampaignRequest;
import com.helpinghands.core.port.in.dto.CampaignResponse;
import com.helpinghands.application.mapper.CampaignMapper;
import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignCategory;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.CampaignSearchCriteria;
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
                new CampaignRequest("New library", "Build it", new BigDecimal("5000"), null, null));

        assertThat(response.title()).isEqualTo("New library");
        assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    void shouldFailWhenOrganizationDoesNotExist() {
        when(organizationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.create(5L,
                new CampaignRequest("X", "Y", BigDecimal.TEN, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateCampaign() {
        Campaign campaign = Campaign.create(organization, "Old", "desc", BigDecimal.TEN);
        when(campaignRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        CampaignResponse response = campaignService.update(1L,
                new CampaignRequest("New", "desc", new BigDecimal("50"), null, CampaignCategory.HEALTH));

        assertThat(response.title()).isEqualTo("New");
        assertThat(response.category()).isEqualTo(CampaignCategory.HEALTH);
    }

    @Test
    void shouldFailUpdatingUnknownCampaign() {
        when(campaignRepository.findByIdForUpdate(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.update(7L,
                new CampaignRequest("New", "desc", BigDecimal.TEN, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldCancelCampaign() {
        Campaign campaign = Campaign.create(organization, "Old", "desc", BigDecimal.TEN);
        when(campaignRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(campaign)).thenReturn(campaign);

        assertThat(campaignService.cancel(1L).status()).isEqualTo(CampaignStatus.CANCELLED);
    }

    @Test
    void shouldSearchCampaigns() {
        Campaign campaign = Campaign.create(organization, "Active one", "desc", BigDecimal.TEN);
        var criteria = new CampaignSearchCriteria(1L, CampaignStatus.ACTIVE, null, "active");
        var query = PageQuery.of(0, 10);
        when(campaignRepository.search(criteria, query)).thenReturn(new PageResult<>(List.of(campaign), 0, 10, 1, 1));

        PageResult<CampaignResponse> result = campaignService.search(criteria, query);

        assertThat(result.content()).extracting(CampaignResponse::title).containsExactly("Active one");
    }
}
