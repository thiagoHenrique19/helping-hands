package com.helpinghands.application.service;

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
import com.helpinghands.core.port.out.ImpactAnalyticsRepository;
import com.helpinghands.core.port.out.OrganizationRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService implements ViewOrganizationAnalyticsUseCase, ViewPlatformAnalyticsUseCase {

    private final ImpactAnalyticsRepository analyticsRepository;
    private final OrganizationRepository organizationRepository;

    public AnalyticsService(ImpactAnalyticsRepository analyticsRepository,
                            OrganizationRepository organizationRepository) {
        this.analyticsRepository = analyticsRepository;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public OrganizationDashboard dashboard(Long organizationId) {
        requireOrganization(organizationId);
        return analyticsRepository.organizationDashboard(organizationId);
    }

    @Override
    public List<MonthlyDonations> donationsByMonth(Long organizationId, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new InvalidRequestException("Both 'from' and 'to' dates are required");
        }
        if (to.isBefore(from)) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
        long months = ChronoUnit.MONTHS.between(from.withDayOfMonth(1), to.withDayOfMonth(1)) + 1;
        if (months > MAX_MONTHS) {
            throw new InvalidRequestException("Date range must not exceed " + MAX_MONTHS + " months");
        }
        requireOrganization(organizationId);
        return analyticsRepository.donationsByMonth(organizationId, from, to);
    }

    @Override
    public List<TopCampaign> topCampaigns(Long organizationId, int limit) {
        validateLimit(limit);
        requireOrganization(organizationId);
        return analyticsRepository.topCampaigns(organizationId, limit);
    }

    @Override
    public List<CampaignProgressRank> campaignProgressRanking(Long organizationId) {
        requireOrganization(organizationId);
        return analyticsRepository.campaignProgressRanking(organizationId);
    }

    @Override
    public List<PaymentMethodBreakdown> donationsByPaymentMethod(Long organizationId) {
        requireOrganization(organizationId);
        return analyticsRepository.donationsByPaymentMethod(organizationId);
    }

    @Override
    public PlatformSummary summary() {
        return analyticsRepository.platformSummary();
    }

    @Override
    public List<TopCampaign> topCampaigns(int limit) {
        validateLimit(limit);
        return analyticsRepository.topCampaigns(null, limit);
    }

    @Override
    public List<PaymentMethodBreakdown> donationsByPaymentMethod() {
        return analyticsRepository.donationsByPaymentMethod(null);
    }

    private void requireOrganization(Long organizationId) {
        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidRequestException("Limit must be between 1 and " + MAX_LIMIT);
        }
    }
}
