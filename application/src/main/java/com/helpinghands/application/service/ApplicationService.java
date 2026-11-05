package com.helpinghands.application.service;

import com.helpinghands.application.mapper.VolunteerMapper;
import com.helpinghands.core.domain.ApplicationStatus;
import com.helpinghands.core.domain.OpportunityStatus;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.VolunteerApplication;
import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.event.OpportunityFilled;
import com.helpinghands.core.event.VolunteerApplicationApproved;
import com.helpinghands.core.exception.ConflictException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.ManageApplicationsUseCase;
import com.helpinghands.core.port.in.dto.ApplicationRequest;
import com.helpinghands.core.port.in.dto.ApplicationResponse;
import com.helpinghands.core.port.out.DomainEventPublisher;
import com.helpinghands.core.port.out.VolunteerApplicationRepository;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService implements ManageApplicationsUseCase {

    private final VolunteerOpportunityRepository opportunityRepository;
    private final VolunteerApplicationRepository applicationRepository;
    private final DomainEventPublisher eventPublisher;
    private final VolunteerMapper mapper;

    public ApplicationService(VolunteerOpportunityRepository opportunityRepository,
                              VolunteerApplicationRepository applicationRepository,
                              DomainEventPublisher eventPublisher,
                              VolunteerMapper mapper) {
        this.opportunityRepository = opportunityRepository;
        this.applicationRepository = applicationRepository;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ApplicationResponse apply(Long opportunityId, ApplicationRequest request) {
        var opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer opportunity", opportunityId));
        String email = Organization.normalizeEmail(request.volunteerEmail());
        if (applicationRepository.existsByOpportunityIdAndVolunteerEmail(opportunityId, email)) {
            throw new ConflictException("A volunteer with email " + email + " already applied to this opportunity");
        }
        var application = VolunteerApplication.apply(opportunity, request.volunteerName(), email,
                request.volunteerPhone(), request.motivation());
        return mapper.toResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ApplicationResponse> listByOpportunity(Long opportunityId, ApplicationStatus status,
                                                             PageQuery pageQuery) {
        opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer opportunity", opportunityId));
        return applicationRepository.findByOpportunityId(opportunityId, status, pageQuery).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public ApplicationResponse approve(Long applicationId) {
        var application = load(applicationId);
        var opportunity = lockOpportunityOf(application);
        application.approve();
        opportunityRepository.save(opportunity);
        var saved = applicationRepository.save(application);
        eventPublisher.publish(new VolunteerApplicationApproved(saved.getId(), opportunity.getId(),
                saved.getVolunteerEmail()));
        if (opportunity.getStatus() == OpportunityStatus.FULL) {
            eventPublisher.publish(new OpportunityFilled(opportunity.getId(), opportunity.getOrganization().getId()));
        }
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ApplicationResponse reject(Long applicationId) {
        var application = load(applicationId);
        application.reject();
        return mapper.toResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional
    public ApplicationResponse withdraw(Long applicationId) {
        var application = load(applicationId);
        var opportunity = lockOpportunityOf(application);
        application.withdraw();
        opportunityRepository.save(opportunity);
        return mapper.toResponse(applicationRepository.save(application));
    }

    private VolunteerApplication load(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer application", id));
    }

    private VolunteerOpportunity lockOpportunityOf(VolunteerApplication application) {
        Long opportunityId = application.getOpportunity().getId();
        return opportunityRepository.findByIdForUpdate(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer opportunity", opportunityId));
    }
}
