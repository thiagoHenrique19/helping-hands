package com.helpinghands.core.port.in;

import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.PublicDonationResponse;

public interface ListDonationsUseCase {

    PageResult<PublicDonationResponse> listByCampaign(Long campaignId, PageQuery pageQuery);
}
