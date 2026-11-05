package com.helpinghands.core.port.in;

import com.helpinghands.core.port.in.dto.analytics.CampaignProgressRank;
import com.helpinghands.core.port.in.dto.analytics.MonthlyDonations;
import com.helpinghands.core.port.in.dto.analytics.OrganizationDashboard;
import com.helpinghands.core.port.in.dto.analytics.PaymentMethodBreakdown;
import com.helpinghands.core.port.in.dto.analytics.TopCampaign;
import java.time.LocalDate;
import java.util.List;

public interface ViewOrganizationAnalyticsUseCase {

    int MAX_MONTHS = 60;
    int MAX_LIMIT = 50;

    OrganizationDashboard dashboard(Long organizationId);

    List<MonthlyDonations> donationsByMonth(Long organizationId, LocalDate from, LocalDate to);

    List<TopCampaign> topCampaigns(Long organizationId, int limit);

    List<CampaignProgressRank> campaignProgressRanking(Long organizationId);

    List<PaymentMethodBreakdown> donationsByPaymentMethod(Long organizationId);
}
