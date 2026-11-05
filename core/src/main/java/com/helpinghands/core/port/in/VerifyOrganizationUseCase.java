package com.helpinghands.core.port.in;

import com.helpinghands.core.port.in.dto.OrganizationResponse;

public interface VerifyOrganizationUseCase {

    OrganizationResponse verify(Long id);
}
