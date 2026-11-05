package com.helpinghands.core.port.in.dto.analytics;

import java.math.BigDecimal;

public record OrganizationDashboard(
        Long organizationId,
        BigDecimal totalRaised,
        long donationsCount,
        long uniqueDonors,
        long activeCampaigns,
        long completedCampaigns,
        long openOpportunities,
        long approvedVolunteers) {
}
