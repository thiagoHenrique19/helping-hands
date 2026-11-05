package com.helpinghands.application.mapper;

import com.helpinghands.core.domain.VolunteerApplication;
import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.port.in.dto.ApplicationResponse;
import com.helpinghands.core.port.in.dto.OpportunityRequest;
import com.helpinghands.core.port.in.dto.OpportunityResponse;
import com.helpinghands.core.domain.Organization;
import org.springframework.stereotype.Component;

@Component
public class VolunteerMapper {

    public VolunteerOpportunity toEntity(Organization organization, OpportunityRequest request) {
        return VolunteerOpportunity.create(organization, request.title(), request.description(),
                request.location(), request.remote(), request.startsAt(), request.endsAt(), request.slots());
    }

    public OpportunityResponse toResponse(VolunteerOpportunity opportunity) {
        return new OpportunityResponse(opportunity.getId(), opportunity.getOrganization().getId(),
                opportunity.getTitle(), opportunity.getDescription(), opportunity.getLocation(),
                opportunity.isRemote(), opportunity.getStartsAt(), opportunity.getEndsAt(), opportunity.getSlots(),
                opportunity.getFilledSlots(), opportunity.availableSlots(), opportunity.getStatus());
    }

    public ApplicationResponse toResponse(VolunteerApplication application) {
        return new ApplicationResponse(application.getId(), application.getOpportunity().getId(),
                application.getVolunteerName(), application.getVolunteerEmail(), application.getVolunteerPhone(),
                application.getMotivation(), application.getStatus(), application.getCreatedAt());
    }
}
