package com.helpinghands.application.service;

import com.helpinghands.application.mapper.VolunteerMapper;
import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.ManageOpportunitiesUseCase;
import com.helpinghands.core.port.in.dto.OpportunityRequest;
import com.helpinghands.core.port.in.dto.OpportunityResponse;
import com.helpinghands.core.port.in.dto.OpportunitySearchCriteria;
import com.helpinghands.core.port.out.OrganizationRepository;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityService implements ManageOpportunitiesUseCase {

    private final VolunteerOpportunityRepository opportunityRepository;
    private final OrganizationRepository organizationRepository;
    private final VolunteerMapper mapper;
    private final Clock clock;

    public OpportunityService(VolunteerOpportunityRepository opportunityRepository,
                              OrganizationRepository organizationRepository,
                              VolunteerMapper mapper,
                              Clock clock) {
        this.opportunityRepository = opportunityRepository;
        this.organizationRepository = organizationRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public OpportunityResponse create(Long organizationId, OpportunityRequest request) {
        var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
        return mapper.toResponse(opportunityRepository.save(mapper.toEntity(organization, request)));
    }

    @Override
    @Transactional(readOnly = true)
    public OpportunityResponse findById(Long id) {
        return mapper.toResponse(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OpportunityResponse> search(OpportunitySearchCriteria criteria, PageQuery pageQuery) {
        return opportunityRepository.search(criteria, clock.instant(), pageQuery).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public OpportunityResponse close(Long id) {
        var opportunity = load(id);
        opportunity.close();
        return mapper.toResponse(opportunityRepository.save(opportunity));
    }

    @Override
    @Transactional
    public OpportunityResponse cancel(Long id) {
        var opportunity = load(id);
        opportunity.cancel();
        return mapper.toResponse(opportunityRepository.save(opportunity));
    }

    private VolunteerOpportunity load(Long id) {
        return opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer opportunity", id));
    }
}
