package com.helpinghands.core.port.in;

import com.helpinghands.core.port.in.dto.OrganizationRequest;
import com.helpinghands.core.port.in.dto.OrganizationResponse;

public interface UpdateOrganizationUseCase {

    OrganizationResponse update(Long id, OrganizationRequest request);
}
