package com.helpinghands.core.port.out;

import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import java.util.Optional;

public interface OrganizationRepository {

    Organization save(Organization organization);

    Optional<Organization> findById(Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    PageResult<Organization> searchByName(String name, PageQuery pageQuery);
}
