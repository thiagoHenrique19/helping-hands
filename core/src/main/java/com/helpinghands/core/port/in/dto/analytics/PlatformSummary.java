package com.helpinghands.core.port.in.dto.analytics;

import java.math.BigDecimal;

public record PlatformSummary(
        long organizations,
        long verifiedOrganizations,
        long campaigns,
        long activeCampaigns,
        BigDecimal totalRaised,
        long donationsCount,
        long uniqueDonors,
        long openOpportunities,
        long approvedVolunteers) {
}
