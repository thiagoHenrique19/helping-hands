package com.helpinghands.core.port.in;

import com.helpinghands.core.domain.ApplicationStatus;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.ApplicationRequest;
import com.helpinghands.core.port.in.dto.ApplicationResponse;

public interface ManageApplicationsUseCase {

    /** @param idempotencyKey optional; repeating a key with the same request returns the first response */
    ApplicationResponse apply(Long opportunityId, ApplicationRequest request, String idempotencyKey);

    PageResult<ApplicationResponse> listByOpportunity(Long opportunityId, ApplicationStatus status,
                                                      PageQuery pageQuery);

    ApplicationResponse approve(Long applicationId);

    ApplicationResponse reject(Long applicationId);

    ApplicationResponse withdraw(Long applicationId);
}
