package com.helpinghands.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.helpinghands.application.mapper.VolunteerMapper;
import com.helpinghands.core.domain.OpportunityStatus;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.exception.BusinessRuleException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.OpportunityRequest;
import com.helpinghands.core.port.in.dto.OpportunityResponse;
import com.helpinghands.core.port.in.dto.OpportunitySearchCriteria;
import com.helpinghands.core.port.out.OrganizationRepository;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    @Mock private VolunteerOpportunityRepository opportunityRepository;
    @Mock private OrganizationRepository organizationRepository;

    private final Instant now = Instant.now();
    private final Organization organization = Organization.register("Org", "o@org.org", "d");
    private OpportunityService service;

    @BeforeEach
    void setUp() {
        service = new OpportunityService(opportunityRepository, organizationRepository, new VolunteerMapper(),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private OpportunityRequest request() {
        Instant startsAt = now.plus(3, ChronoUnit.DAYS);
        return new OpportunityRequest("Tutoring", "Help kids", "School", false, startsAt,
                startsAt.plus(2, ChronoUnit.HOURS), 4);
    }

    private VolunteerOpportunity existing() {
        return new VolunteerMapper().toEntity(organization, request());
    }

    @Test
    void shouldCreateOpportunity() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(opportunityRepository.save(any(VolunteerOpportunity.class))).thenAnswer(i -> i.getArgument(0));

        OpportunityResponse response = service.create(1L, request());

        assertThat(response.title()).isEqualTo("Tutoring");
        assertThat(response.status()).isEqualTo(OpportunityStatus.OPEN);
        assertThat(response.availableSlots()).isEqualTo(4);
    }

    @Test
    void shouldFailCreatingForUnknownOrganization() {
        when(organizationRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(9L, request())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFailFindingUnknownOpportunity() {
        when(opportunityRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldCloseOpportunity() {
        var opportunity = existing();
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opportunity));
        when(opportunityRepository.save(opportunity)).thenReturn(opportunity);

        assertThat(service.close(1L).status()).isEqualTo(OpportunityStatus.CLOSED);
    }

    @Test
    void shouldCancelOpportunityOnlyOnce() {
        var opportunity = existing();
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opportunity));
        when(opportunityRepository.save(opportunity)).thenReturn(opportunity);

        assertThat(service.cancel(1L).status()).isEqualTo(OpportunityStatus.CANCELLED);
        assertThatThrownBy(() -> service.cancel(1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldSearchUsingCurrentInstantForUpcomingFilter() {
        var criteria = new OpportunitySearchCriteria(1L, OpportunityStatus.OPEN, true, true);
        var query = PageQuery.of(0, 10);
        when(opportunityRepository.search(criteria, now, query))
                .thenReturn(new PageResult<>(List.of(existing()), 0, 10, 1, 1));

        assertThat(service.search(criteria, query).content()).hasSize(1);
    }
}
