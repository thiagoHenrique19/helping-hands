package com.helpinghands.core.port.in;

import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.OpportunityRequest;
import com.helpinghands.core.port.in.dto.OpportunityResponse;
import com.helpinghands.core.port.in.dto.OpportunitySearchCriteria;

public interface ManageOpportunitiesUseCase {

    OpportunityResponse create(Long organizationId, OpportunityRequest request);

    OpportunityResponse findById(Long id);

    PageResult<OpportunityResponse> search(OpportunitySearchCriteria criteria, PageQuery pageQuery);

    OpportunityResponse close(Long id);

    OpportunityResponse cancel(Long id);
}
