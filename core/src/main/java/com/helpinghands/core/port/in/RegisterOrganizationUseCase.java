package com.helpinghands.core.port.in;

import com.helpinghands.core.port.in.dto.OrganizationRequest;
import com.helpinghands.core.port.in.dto.OrganizationResponse;

public interface RegisterOrganizationUseCase {

    /**
     * Registers an organization. When {@code ownerUserId} is present the organization is linked to that
     * user, who becomes its owner.
     */
    OrganizationResponse register(OrganizationRequest request, Long ownerUserId);

    default OrganizationResponse register(OrganizationRequest request) {
        return register(request, null);
    }

    OrganizationResponse findById(Long id);
}
