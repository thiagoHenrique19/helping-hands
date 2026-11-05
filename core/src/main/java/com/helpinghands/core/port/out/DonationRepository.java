package com.helpinghands.core.port.out;

import com.helpinghands.core.domain.Donation;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.TopDonorResponse;
import java.util.List;

public interface DonationRepository {

    Donation save(Donation donation);

    PageResult<Donation> findByCampaignId(Long campaignId, PageQuery pageQuery);

    List<TopDonorResponse> findTopDonors(Long campaignId, int limit);
}
