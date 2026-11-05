package com.helpinghands.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.helpinghands.core.port.in.ViewOrganizationAnalyticsUseCase;
import com.helpinghands.core.port.in.ViewPlatformAnalyticsUseCase;
import com.helpinghands.core.port.in.dto.analytics.CampaignProgressRank;
import com.helpinghands.core.port.in.dto.analytics.MonthlyDonations;
import com.helpinghands.core.port.in.dto.analytics.OrganizationDashboard;
import com.helpinghands.core.port.in.dto.analytics.PaymentMethodBreakdown;
import com.helpinghands.core.port.in.dto.analytics.PlatformSummary;
import com.helpinghands.core.port.in.dto.analytics.TopCampaign;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Analytics", description = "Impact reports backed by native PostgreSQL queries")
@RequestMapping("/api")
public class AnalyticsController {

    private final ViewOrganizationAnalyticsUseCase organizationAnalytics;
    private final ViewPlatformAnalyticsUseCase platformAnalytics;

    public AnalyticsController(ViewOrganizationAnalyticsUseCase organizationAnalytics,
                               ViewPlatformAnalyticsUseCase platformAnalytics) {
        this.organizationAnalytics = organizationAnalytics;
        this.platformAnalytics = platformAnalytics;
    }

    @Operation(summary = "Organization dashboard")
    @GetMapping("/organizations/{organizationId}/analytics/dashboard")
    @PreAuthorize("hasRole('ADMIN') or @access.ownsOrganization(authentication, #organizationId)")
    public OrganizationDashboard dashboard(@PathVariable Long organizationId) {
        return organizationAnalytics.dashboard(organizationId);
    }

    @Operation(summary = "Donations per month for a date range")
    @GetMapping("/organizations/{organizationId}/analytics/donations-by-month")
    @PreAuthorize("hasRole('ADMIN') or @access.ownsOrganization(authentication, #organizationId)")
    public List<MonthlyDonations> donationsByMonth(
            @PathVariable Long organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return organizationAnalytics.donationsByMonth(organizationId, from, to);
    }

    @Operation(summary = "Top campaigns of an organization by raised amount")
    @GetMapping("/organizations/{organizationId}/analytics/top-campaigns")
    @PreAuthorize("hasRole('ADMIN') or @access.ownsOrganization(authentication, #organizationId)")
    public List<TopCampaign> topCampaigns(@PathVariable Long organizationId,
                                          @RequestParam(defaultValue = "10") int limit) {
        return organizationAnalytics.topCampaigns(organizationId, limit);
    }

    @Operation(summary = "Campaign progress ranking of an organization")
    @GetMapping("/organizations/{organizationId}/analytics/campaign-progress")
    @PreAuthorize("hasRole('ADMIN') or @access.ownsOrganization(authentication, #organizationId)")
    public List<CampaignProgressRank> campaignProgress(@PathVariable Long organizationId) {
        return organizationAnalytics.campaignProgressRanking(organizationId);
    }

    @Operation(summary = "Donations by payment method for an organization")
    @GetMapping("/organizations/{organizationId}/analytics/payment-methods")
    @PreAuthorize("hasRole('ADMIN') or @access.ownsOrganization(authentication, #organizationId)")
    public List<PaymentMethodBreakdown> paymentMethods(@PathVariable Long organizationId) {
        return organizationAnalytics.donationsByPaymentMethod(organizationId);
    }

    @Operation(summary = "Platform-wide summary")
    @GetMapping("/analytics/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public PlatformSummary summary() {
        return platformAnalytics.summary();
    }

    @Operation(summary = "Top campaigns of the platform")
    @GetMapping("/analytics/top-campaigns")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TopCampaign> platformTopCampaigns(@RequestParam(defaultValue = "10") int limit) {
        return platformAnalytics.topCampaigns(limit);
    }

    @Operation(summary = "Donations by payment method on the platform")
    @GetMapping("/analytics/payment-methods")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PaymentMethodBreakdown> platformPaymentMethods() {
        return platformAnalytics.donationsByPaymentMethod();
    }
}
