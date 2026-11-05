package com.helpinghands.core.port.in;

import com.helpinghands.core.port.in.dto.analytics.PaymentMethodBreakdown;
import com.helpinghands.core.port.in.dto.analytics.PlatformSummary;
import com.helpinghands.core.port.in.dto.analytics.TopCampaign;
import java.util.List;

public interface ViewPlatformAnalyticsUseCase {

    PlatformSummary summary();

    List<TopCampaign> topCampaigns(int limit);

    List<PaymentMethodBreakdown> donationsByPaymentMethod();
}
