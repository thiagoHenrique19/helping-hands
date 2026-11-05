package com.helpinghands.core.port.out;

import com.helpinghands.core.port.in.dto.analytics.CampaignProgressRank;
import com.helpinghands.core.port.in.dto.analytics.MonthlyDonations;
import com.helpinghands.core.port.in.dto.analytics.OrganizationDashboard;
import com.helpinghands.core.port.in.dto.analytics.PaymentMethodBreakdown;
import com.helpinghands.core.port.in.dto.analytics.PlatformSummary;
import com.helpinghands.core.port.in.dto.analytics.TopCampaign;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only reporting port. Implementations are free to use database specific aggregations.
 * Methods taking a nullable {@code organizationId} are scoped to one organization when it is
 * present and platform-wide otherwise.
 */
public interface ImpactAnalyticsRepository {

    OrganizationDashboard organizationDashboard(Long organizationId);

    PlatformSummary platformSummary();

    List<TopCampaign> topCampaigns(Long organizationId, int limit);

    List<MonthlyDonations> donationsByMonth(Long organizationId, LocalDate from, LocalDate to);

    List<CampaignProgressRank> campaignProgressRanking(Long organizationId);

    List<PaymentMethodBreakdown> donationsByPaymentMethod(Long organizationId);
}
