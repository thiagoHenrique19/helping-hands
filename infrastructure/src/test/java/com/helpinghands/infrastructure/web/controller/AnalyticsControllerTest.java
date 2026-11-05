package com.helpinghands.infrastructure.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.domain.PaymentMethod;
import com.helpinghands.core.exception.InvalidRequestException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.port.in.ViewOrganizationAnalyticsUseCase;
import com.helpinghands.core.port.in.ViewPlatformAnalyticsUseCase;
import com.helpinghands.core.port.in.dto.analytics.CampaignProgressRank;
import com.helpinghands.core.port.in.dto.analytics.MonthlyDonations;
import com.helpinghands.core.port.in.dto.analytics.OrganizationDashboard;
import com.helpinghands.core.port.in.dto.analytics.PaymentMethodBreakdown;
import com.helpinghands.core.port.in.dto.analytics.PlatformSummary;
import com.helpinghands.core.port.in.dto.analytics.TopCampaign;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ViewOrganizationAnalyticsUseCase organizationAnalytics;
    @MockBean private ViewPlatformAnalyticsUseCase platformAnalytics;

    @Test
    void shouldReturnDashboard() throws Exception {
        when(organizationAnalytics.dashboard(1L))
                .thenReturn(new OrganizationDashboard(1L, new BigDecimal("200.00"), 3, 2, 2, 1, 1, 2));

        mockMvc.perform(get("/api/organizations/1/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRaised").value(200.00))
                .andExpect(jsonPath("$.uniqueDonors").value(2))
                .andExpect(jsonPath("$.approvedVolunteers").value(2));
    }

    @Test
    void shouldReturnNotFoundForUnknownOrganization() throws Exception {
        when(organizationAnalytics.dashboard(9L)).thenThrow(new ResourceNotFoundException("Organization", 9L));

        mockMvc.perform(get("/api/organizations/9/analytics/dashboard")).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnDonationsByMonth() throws Exception {
        when(organizationAnalytics.donationsByMonth(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of(new MonthlyDonations(LocalDate.of(2026, 1, 1), 2, new BigDecimal("140.00")),
                        new MonthlyDonations(LocalDate.of(2026, 2, 1), 0, BigDecimal.ZERO)));

        mockMvc.perform(get("/api/organizations/1/analytics/donations-by-month?from=2026-01-01&to=2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].month").value("2026-01-01"))
                .andExpect(jsonPath("$[0].donationsCount").value(2));
    }

    @Test
    void shouldRejectMissingOrMalformedDates() throws Exception {
        mockMvc.perform(get("/api/organizations/1/analytics/donations-by-month?from=2026-01-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/organizations/1/analytics/donations-by-month?from=yesterday&to=today"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidRangeReportedByUseCase() throws Exception {
        when(organizationAnalytics.donationsByMonth(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1)))
                .thenThrow(new InvalidRequestException("'from' must not be after 'to'"));

        mockMvc.perform(get("/api/organizations/1/analytics/donations-by-month?from=2026-03-01&to=2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnTopCampaignsAndRanking() throws Exception {
        when(organizationAnalytics.topCampaigns(1L, 3))
                .thenReturn(List.of(new TopCampaign(5L, 1L, "Big", BigDecimal.TEN, BigDecimal.TEN)));
        when(organizationAnalytics.campaignProgressRanking(1L)).thenReturn(List.of(
                new CampaignProgressRank(1, 5L, "Big", CampaignStatus.COMPLETED, BigDecimal.TEN, BigDecimal.TEN,
                        new BigDecimal("100.00"))));

        mockMvc.perform(get("/api/organizations/1/analytics/top-campaigns?limit=3"))
                .andExpect(jsonPath("$[0].title").value("Big"));
        mockMvc.perform(get("/api/organizations/1/analytics/campaign-progress"))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void shouldReturnPaymentMethodBreakdown() throws Exception {
        when(organizationAnalytics.donationsByPaymentMethod(1L))
                .thenReturn(List.of(new PaymentMethodBreakdown(PaymentMethod.PIX, 2, new BigDecimal("160.00"))));

        mockMvc.perform(get("/api/organizations/1/analytics/payment-methods"))
                .andExpect(jsonPath("$[0].paymentMethod").value("PIX"));
    }

    @Test
    void shouldReturnPlatformWideReports() throws Exception {
        when(platformAnalytics.summary()).thenReturn(new PlatformSummary(2, 1, 4, 3, new BigDecimal("300"), 4, 3, 2, 2));
        when(platformAnalytics.topCampaigns(10)).thenReturn(List.of());
        when(platformAnalytics.donationsByPaymentMethod()).thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/summary")).andExpect(jsonPath("$.organizations").value(2));
        mockMvc.perform(get("/api/analytics/top-campaigns")).andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/payment-methods")).andExpect(status().isOk());
    }
}
