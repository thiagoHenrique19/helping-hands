package com.helpinghands.core.port.in;

import com.helpinghands.core.domain.ApplicationStatus;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.ApplicationRequest;
import com.helpinghands.core.port.in.dto.ApplicationResponse;

public interface ManageApplicationsUseCase {

    ApplicationResponse apply(Long opportunityId, ApplicationRequest request);

    PageResult<ApplicationResponse> listByOpportunity(Long opportunityId, ApplicationStatus status,
                                                      PageQuery pageQuery);

    ApplicationResponse approve(Long applicationId);

    ApplicationResponse reject(Long applicationId);

    ApplicationResponse withdraw(Long applicationId);
}
