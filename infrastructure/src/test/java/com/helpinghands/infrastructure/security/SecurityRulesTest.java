package com.helpinghands.infrastructure.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.domain.CampaignCategory;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.AuthenticateUserUseCase;
import com.helpinghands.core.port.in.ListDonationsUseCase;
import com.helpinghands.core.port.in.ListOrganizationsUseCase;
import com.helpinghands.core.port.in.MakeDonationUseCase;
import com.helpinghands.core.port.in.ManageApplicationsUseCase;
import com.helpinghands.core.port.in.ManageCampaignsUseCase;
import com.helpinghands.core.port.in.ManageOpportunitiesUseCase;
import com.helpinghands.core.port.in.RegisterOrganizationUseCase;
import com.helpinghands.core.port.in.RegisterUserUseCase;
import com.helpinghands.core.port.in.UpdateOrganizationUseCase;
import com.helpinghands.core.port.in.VerifyOrganizationUseCase;
import com.helpinghands.core.port.in.ViewOrganizationAnalyticsUseCase;
import com.helpinghands.core.port.in.ViewPlatformAnalyticsUseCase;
import com.helpinghands.core.port.in.ViewTopDonorsUseCase;
import com.helpinghands.core.port.in.dto.CampaignResponse;
import com.helpinghands.core.port.in.dto.DonationResponse;
import com.helpinghands.core.port.in.dto.OrganizationResponse;
import com.helpinghands.core.port.in.dto.analytics.PlatformSummary;
import com.helpinghands.infrastructure.web.controller.AnalyticsController;
import com.helpinghands.infrastructure.web.controller.ApplicationController;
import com.helpinghands.infrastructure.web.controller.AuthController;
import com.helpinghands.infrastructure.web.controller.CampaignController;
import com.helpinghands.infrastructure.web.controller.DonationController;
import com.helpinghands.infrastructure.web.controller.OpportunityController;
import com.helpinghands.infrastructure.web.controller.OrganizationController;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({OrganizationController.class, CampaignController.class, DonationController.class,
        OpportunityController.class, ApplicationController.class, AnalyticsController.class, AuthController.class})
@Import(WebMvcSecurityTestConfig.class)
class SecurityRulesTest {

    private static final String ORGANIZATION_BODY = "{\"name\":\"Hope\",\"email\":\"hope@ngo.org\"}";
    private static final String CAMPAIGN_BODY = "{\"title\":\"Books\",\"goalAmount\":100}";
    private static final String DONATION_BODY =
            "{\"donorName\":\"A\",\"donorEmail\":\"a@a.com\",\"amount\":10,\"paymentMethod\":\"PIX\"}";
    private static final String APPLICATION_BODY = "{\"volunteerName\":\"V\",\"volunteerEmail\":\"v@mail.com\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private AccessPolicy access;
    @Autowired private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @MockBean private RegisterOrganizationUseCase registerOrganization;
    @MockBean private UpdateOrganizationUseCase updateOrganization;
    @MockBean private VerifyOrganizationUseCase verifyOrganization;
    @MockBean private ListOrganizationsUseCase listOrganizations;
    @MockBean private ManageCampaignsUseCase campaigns;
    @MockBean private MakeDonationUseCase donations;
    @MockBean private ListDonationsUseCase listDonations;
    @MockBean private ViewTopDonorsUseCase topDonors;
    @MockBean private ManageOpportunitiesUseCase opportunities;
    @MockBean private ManageApplicationsUseCase applications;
    @MockBean private ViewOrganizationAnalyticsUseCase organizationAnalytics;
    @MockBean private ViewPlatformAnalyticsUseCase platformAnalytics;
    @MockBean private RegisterUserUseCase registerUser;
    @MockBean private AuthenticateUserUseCase authenticateUser;

    @AfterEach
    void resetAccessPolicy() {
        reset(access);
    }

    private CampaignResponse campaign() {
        return new CampaignResponse(1L, 2L, "Books", "d", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                CampaignStatus.ACTIVE, CampaignCategory.OTHER, null);
    }

    @Test
    void shouldAllowAnonymousReadsOfPublicResources() throws Exception {
        when(campaigns.findById(1L)).thenReturn(campaign());
        when(campaigns.search(any(), any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
        when(organizationAnalytics.dashboard(anyLong())).thenReturn(null);

        mockMvc.perform(get("/api/campaigns/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/campaigns")).andExpect(status().isOk());
        mockMvc.perform(get("/api/organizations/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/opportunities")).andExpect(status().isOk());
        mockMvc.perform(get("/api/campaigns/1/donations")).andExpect(status().isOk());
        mockMvc.perform(get("/api/campaigns/1/top-donors")).andExpect(status().isOk());
    }

    @Test
    void shouldAllowAnonymousDonationsApplicationsAndWithdrawals() throws Exception {
        when(donations.donate(anyLong(), any(), any())).thenReturn(new DonationResponse(1L, 1L, "A", BigDecimal.TEN,
                Instant.now(), "DON-1", false, null, com.helpinghands.core.domain.PaymentMethod.PIX));

        mockMvc.perform(post("/api/campaigns/1/donations").contentType(MediaType.APPLICATION_JSON).content(DONATION_BODY))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/opportunities/1/applications").contentType(MediaType.APPLICATION_JSON)
                        .content(APPLICATION_BODY)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/applications/1/withdraw")).andExpect(status().isOk());
    }

    @Test
    void shouldRequireAuthenticationForWrites() throws Exception {
        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON).content(ORGANIZATION_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.type").value("urn:helping-hands:problem:unauthorized"));
        mockMvc.perform(post("/api/organizations/1/campaigns").contentType(MediaType.APPLICATION_JSON)
                .content(CAMPAIGN_BODY)).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/campaigns/1").contentType(MediaType.APPLICATION_JSON).content(CAMPAIGN_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/campaigns/1/cancel")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/organizations/1/verify")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/applications/1/approve")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/opportunities/1/close")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireAuthenticationForAnalyticsAndApplicationListing() throws Exception {
        mockMvc.perform(get("/api/organizations/1/analytics/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/analytics/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/opportunities/1/applications")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "5", roles = "VOLUNTEER")
    void shouldForbidVolunteersFromManagingOrganizations() throws Exception {
        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON).content(ORGANIZATION_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:helping-hands:problem:forbidden"));
        mockMvc.perform(post("/api/organizations/1/campaigns").contentType(MediaType.APPLICATION_JSON)
                .content(CAMPAIGN_BODY)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/organizations/1/analytics/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "5", roles = "ORGANIZATION")
    void shouldAllowOrganizationUsersToManageOnlyWhatTheyOwn() throws Exception {
        when(access.ownsOrganization(any(), org.mockito.ArgumentMatchers.eq(1L))).thenReturn(true);
        when(access.ownsCampaign(any(), org.mockito.ArgumentMatchers.eq(1L))).thenReturn(true);
        when(campaigns.create(anyLong(), any())).thenReturn(campaign());
        when(campaigns.cancel(1L)).thenReturn(campaign());

        mockMvc.perform(post("/api/organizations/1/campaigns").contentType(MediaType.APPLICATION_JSON)
                .content(CAMPAIGN_BODY)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/campaigns/1/cancel")).andExpect(status().isOk());

        mockMvc.perform(post("/api/organizations/2/campaigns").contentType(MediaType.APPLICATION_JSON)
                .content(CAMPAIGN_BODY)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/campaigns/2/cancel")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/applications/9/approve")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/analytics/summary")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "5", roles = "ORGANIZATION")
    void shouldLinkNewOrganizationToTheAuthenticatedOrganizationUser() throws Exception {
        when(registerOrganization.register(any(), org.mockito.ArgumentMatchers.eq(5L)))
                .thenReturn(new OrganizationResponse(1L, "Hope", "hope@ngo.org", null, null, null, false));

        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON).content(ORGANIZATION_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void shouldAllowAdministratorsEverywhere() throws Exception {
        when(platformAnalytics.summary()).thenReturn(new PlatformSummary(0, 0, 0, 0, BigDecimal.ZERO, 0, 0, 0, 0));
        when(campaigns.cancel(1L)).thenReturn(campaign());
        when(registerOrganization.register(any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new OrganizationResponse(1L, "Hope", "hope@ngo.org", null, null, null, false));

        mockMvc.perform(get("/api/analytics/summary")).andExpect(status().isOk());
        mockMvc.perform(post("/api/campaigns/1/cancel")).andExpect(status().isOk());
        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON).content(ORGANIZATION_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectMalformedBearerTokens() throws Exception {
        when(jwtDecoder.decode("garbage"))
                .thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("malformed"));
        mockMvc.perform(post("/api/campaigns/1/cancel").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }
}
