package com.helpinghands.core.port.out;

import com.helpinghands.core.domain.Organization;
import java.util.Optional;

public interface OrganizationRepository {

    Organization save(Organization organization);

    Optional<Organization> findById(Long id);
}
