package com.helpinghands.core.port.in;

import com.helpinghands.core.port.in.dto.CampaignRequest;
import com.helpinghands.core.port.in.dto.CampaignResponse;
import java.util.List;

public interface ManageCampaignsUseCase {

    CampaignResponse create(Long organizationId, CampaignRequest request);

    CampaignResponse findById(Long id);

    List<CampaignResponse> listActive();
}
