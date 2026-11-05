package com.helpinghands.core.port.in;

import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.CampaignRequest;
import com.helpinghands.core.port.in.dto.CampaignResponse;
import com.helpinghands.core.port.in.dto.CampaignSearchCriteria;

public interface ManageCampaignsUseCase {

    CampaignResponse create(Long organizationId, CampaignRequest request);

    CampaignResponse findById(Long id);

    CampaignResponse update(Long id, CampaignRequest request);

    CampaignResponse cancel(Long id);

    PageResult<CampaignResponse> search(CampaignSearchCriteria criteria, PageQuery pageQuery);
}
