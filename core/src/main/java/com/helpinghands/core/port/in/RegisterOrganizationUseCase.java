package com.helpinghands.core.port.in;

import com.helpinghands.core.port.in.dto.OrganizationRequest;
import com.helpinghands.core.port.in.dto.OrganizationResponse;

public interface RegisterOrganizationUseCase {

    OrganizationResponse register(OrganizationRequest request);

    OrganizationResponse findById(Long id);
}
