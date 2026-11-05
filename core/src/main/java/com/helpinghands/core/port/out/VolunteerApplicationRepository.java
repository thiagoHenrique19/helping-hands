package com.helpinghands.core.port.out;

import com.helpinghands.core.domain.ApplicationStatus;
import com.helpinghands.core.domain.VolunteerApplication;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import java.util.Optional;

public interface VolunteerApplicationRepository {

    VolunteerApplication save(VolunteerApplication application);

    Optional<VolunteerApplication> findById(Long id);

    boolean existsByOpportunityIdAndVolunteerEmail(Long opportunityId, String volunteerEmail);

    PageResult<VolunteerApplication> findByOpportunityId(Long opportunityId, ApplicationStatus status,
                                                         PageQuery pageQuery);
}
