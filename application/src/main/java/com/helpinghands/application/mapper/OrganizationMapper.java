package com.helpinghands.application.mapper;

import com.helpinghands.core.port.in.dto.OrganizationRequest;
import com.helpinghands.core.port.in.dto.OrganizationResponse;
import com.helpinghands.core.domain.Organization;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    public Organization toEntity(OrganizationRequest request) {
        return Organization.register(request.name(), request.email(), request.description(),
                request.phone(), request.website());
    }

    public OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(organization.getId(), organization.getName(), organization.getEmail(),
                organization.getDescription(), organization.getPhone(), organization.getWebsite(),
                organization.isVerified());
    }
}
