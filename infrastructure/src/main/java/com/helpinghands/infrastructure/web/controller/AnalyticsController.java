package com.helpinghands.infrastructure.web.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final ViewOrganizationAnalyticsUseCase organizationAnalytics;
    private final ViewPlatformAnalyticsUseCase platformAnalytics;

    public AnalyticsController(ViewOrganizationAnalyticsUseCase organizationAnalytics,
                               ViewPlatformAnalyticsUseCase platformAnalytics) {
        this.organizationAnalytics = organizationAnalytics;
        this.platformAnalytics = platformAnalytics;
    }

    @GetMapping("/organizations/{organizationId}/analytics/dashboard")
    public OrganizationDashboard dashboard(@PathVariable Long organizationId) {
        return organizationAnalytics.dashboard(organizationId);
    }

    @GetMapping("/organizations/{organizationId}/analytics/donations-by-month")
    public List<MonthlyDonations> donationsByMonth(
            @PathVariable Long organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return organizationAnalytics.donationsByMonth(organizationId, from, to);
    }

    @GetMapping("/organizations/{organizationId}/analytics/top-campaigns")
    public List<TopCampaign> topCampaigns(@PathVariable Long organizationId,
                                          @RequestParam(defaultValue = "10") int limit) {
        return organizationAnalytics.topCampaigns(organizationId, limit);
    }

    @GetMapping("/organizations/{organizationId}/analytics/campaign-progress")
    public List<CampaignProgressRank> campaignProgress(@PathVariable Long organizationId) {
        return organizationAnalytics.campaignProgressRanking(organizationId);
    }

    @GetMapping("/organizations/{organizationId}/analytics/payment-methods")
    public List<PaymentMethodBreakdown> paymentMethods(@PathVariable Long organizationId) {
        return organizationAnalytics.donationsByPaymentMethod(organizationId);
    }

    @GetMapping("/analytics/summary")
    public PlatformSummary summary() {
        return platformAnalytics.summary();
    }

    @GetMapping("/analytics/top-campaigns")
    public List<TopCampaign> platformTopCampaigns(@RequestParam(defaultValue = "10") int limit) {
        return platformAnalytics.topCampaigns(limit);
    }

    @GetMapping("/analytics/payment-methods")
    public List<PaymentMethodBreakdown> platformPaymentMethods() {
        return platformAnalytics.donationsByPaymentMethod();
    }
}
