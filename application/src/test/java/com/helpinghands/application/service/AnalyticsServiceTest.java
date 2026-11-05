package com.helpinghands.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.exception.InvalidRequestException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.port.in.dto.analytics.MonthlyDonations;
import com.helpinghands.core.port.in.dto.analytics.OrganizationDashboard;
import com.helpinghands.core.port.in.dto.analytics.PlatformSummary;
import com.helpinghands.core.port.in.dto.analytics.TopCampaign;
import com.helpinghands.core.port.out.ImpactAnalyticsRepository;
import com.helpinghands.core.port.out.OrganizationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private ImpactAnalyticsRepository analyticsRepository;
    @Mock private OrganizationRepository organizationRepository;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(analyticsRepository, organizationRepository);
    }

    private void organizationExists() {
        when(organizationRepository.findById(1L))
                .thenReturn(Optional.of(Organization.register("Org", "o@org.org", "d")));
    }

    @Test
    void shouldReturnDashboardOfExistingOrganization() {
        organizationExists();
        var dashboard = new OrganizationDashboard(1L, BigDecimal.TEN, 2, 2, 1, 0, 3, 4);
        when(analyticsRepository.organizationDashboard(1L)).thenReturn(dashboard);

        assertThat(service.dashboard(1L)).isEqualTo(dashboard);
    }

    @Test
    void shouldFailForUnknownOrganization() {
        when(organizationRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.dashboard(9L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(analyticsRepository);
    }

    @Test
    void shouldReturnDonationsByMonthForValidRange() {
        organizationExists();
        var from = LocalDate.of(2026, 1, 15);
        var to = LocalDate.of(2026, 3, 2);
        var months = List.of(new MonthlyDonations(LocalDate.of(2026, 1, 1), 1, BigDecimal.TEN));
        when(analyticsRepository.donationsByMonth(1L, from, to)).thenReturn(months);

        assertThat(service.donationsByMonth(1L, from, to)).isEqualTo(months);
    }

    @Test
    void shouldRejectInvertedDateRange() {
        assertThatThrownBy(() -> service.donationsByMonth(1L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1)))
                .isInstanceOf(InvalidRequestException.class);
        verify(analyticsRepository, never()).donationsByMonth(1L, null, null);
    }

    @Test
    void shouldRejectMissingDates() {
        assertThatThrownBy(() -> service.donationsByMonth(1L, null, LocalDate.now()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shouldRejectRangeLongerThanMaximum() {
        assertThatThrownBy(() -> service.donationsByMonth(1L, LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("60");
    }

    @Test
    void shouldValidateTopCampaignsLimit() {
        assertThatThrownBy(() -> service.topCampaigns(0)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.topCampaigns(1L, 51)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shouldReturnPlatformWideTopCampaignsWithoutOrganizationScope() {
        var top = List.of(new TopCampaign(1L, 2L, "T", BigDecimal.TEN, BigDecimal.TEN));
        when(analyticsRepository.topCampaigns(null, 5)).thenReturn(top);

        assertThat(service.topCampaigns(5)).isEqualTo(top);
    }

    @Test
    void shouldReturnPlatformSummary() {
        var summary = new PlatformSummary(1, 1, 1, 1, BigDecimal.ONE, 1, 1, 1, 1);
        when(analyticsRepository.platformSummary()).thenReturn(summary);

        assertThat(service.summary()).isEqualTo(summary);
    }
}
