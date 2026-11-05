package com.helpinghands.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpinghands.application.mapper.VolunteerMapper;
import com.helpinghands.core.domain.ApplicationStatus;
import com.helpinghands.core.domain.OpportunityStatus;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.VolunteerApplication;
import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.event.OpportunityFilled;
import com.helpinghands.core.event.VolunteerApplicationApproved;
import com.helpinghands.core.exception.BusinessRuleException;
import com.helpinghands.core.exception.ConflictException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.ApplicationRequest;
import com.helpinghands.core.port.in.dto.ApplicationResponse;
import com.helpinghands.core.port.out.DomainEventPublisher;
import com.helpinghands.core.port.out.VolunteerApplicationRepository;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private VolunteerOpportunityRepository opportunityRepository;
    @Mock private VolunteerApplicationRepository applicationRepository;
    @Mock private DomainEventPublisher eventPublisher;

    private ApplicationService service;
    private final ApplicationRequest request = new ApplicationRequest("Vera", "Vera@mail.com", "1199", "I care");

    @BeforeEach
    void setUp() {
        service = new ApplicationService(opportunityRepository, applicationRepository, eventPublisher,
                new VolunteerMapper());
    }

    private VolunteerOpportunity opportunity(int slots) {
        Instant startsAt = Instant.now().plus(2, ChronoUnit.DAYS);
        return VolunteerOpportunity.create(Organization.register("Org", "o@org.org", "d"), "T", "d", "L", false,
                startsAt, startsAt.plus(1, ChronoUnit.HOURS), slots);
    }

    @Test
    void shouldApplyToOpportunity() {
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opportunity(2)));
        when(applicationRepository.existsByOpportunityIdAndVolunteerEmail(1L, "vera@mail.com")).thenReturn(false);
        when(applicationRepository.save(any(VolunteerApplication.class))).thenAnswer(i -> i.getArgument(0));

        ApplicationResponse response = service.apply(1L, request);

        assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(response.volunteerEmail()).isEqualTo("vera@mail.com");
    }

    @Test
    void shouldRejectDuplicateApplicationByEmail() {
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opportunity(2)));
        when(applicationRepository.existsByOpportunityIdAndVolunteerEmail(1L, "vera@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.apply(1L, request)).isInstanceOf(ConflictException.class);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void shouldRejectApplicationToClosedOpportunity() {
        var closed = opportunity(2);
        closed.close();
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(closed));
        when(applicationRepository.existsByOpportunityIdAndVolunteerEmail(1L, "vera@mail.com")).thenReturn(false);

        assertThatThrownBy(() -> service.apply(1L, request)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldFailApplyingToUnknownOpportunity() {
        when(opportunityRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(9L, request)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldApproveAndPublishEvents() {
        var opportunity = opportunity(1);
        var application = VolunteerApplication.apply(opportunity, "Vera", "vera@mail.com", null, null);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = service.approve(5L);

        assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.FULL);
        verify(eventPublisher).publish(any(VolunteerApplicationApproved.class));
        verify(eventPublisher).publish(any(OpportunityFilled.class));
    }

    @Test
    void shouldNotPublishFilledEventWhileSlotsRemain() {
        var application = VolunteerApplication.apply(opportunity(2), "Vera", "vera@mail.com", null, null);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        service.approve(5L);

        verify(eventPublisher).publish(any(VolunteerApplicationApproved.class));
        verify(eventPublisher, never()).publish(any(OpportunityFilled.class));
    }

    @Test
    void shouldNotPublishEventsWhenApprovalFails() {
        var opportunity = opportunity(1);
        VolunteerApplication.apply(opportunity, "A", "a@mail.com", null, null).approve();
        var second = VolunteerApplication.apply(opportunity, "B", "b@mail.com", null, null);
        when(applicationRepository.findById(6L)).thenReturn(Optional.of(second));

        assertThatThrownBy(() -> service.approve(6L)).isInstanceOf(BusinessRuleException.class);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectApplication() {
        var application = VolunteerApplication.apply(opportunity(1), "Vera", "vera@mail.com", null, null);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        assertThat(service.reject(5L).status()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void shouldWithdrawApprovedApplicationAndReopenOpportunity() {
        var opportunity = opportunity(1);
        var application = VolunteerApplication.apply(opportunity, "Vera", "vera@mail.com", null, null);
        application.approve();
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        assertThat(service.withdraw(5L).status()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.OPEN);
        verify(opportunityRepository).save(opportunity);
    }

    @Test
    void shouldListApplicationsOfOpportunityByStatus() {
        var query = PageQuery.of(0, 10);
        var application = VolunteerApplication.apply(opportunity(1), "Vera", "vera@mail.com", null, null);
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opportunity(1)));
        when(applicationRepository.findByOpportunityId(1L, ApplicationStatus.PENDING, query))
                .thenReturn(new PageResult<>(List.of(application), 0, 10, 1, 1));

        assertThat(service.listByOpportunity(1L, ApplicationStatus.PENDING, query).content()).hasSize(1);
    }

    @Test
    void shouldFailWhenApplicationDoesNotExist() {
        when(applicationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdraw(404L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
