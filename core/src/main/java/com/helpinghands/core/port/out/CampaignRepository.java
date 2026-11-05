package com.helpinghands.core.port.out;

import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.CampaignSearchCriteria;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CampaignRepository {

    Campaign save(Campaign campaign);

    Optional<Campaign> findById(Long id);

    PageResult<Campaign> search(CampaignSearchCriteria criteria, PageQuery pageQuery);

    List<Campaign> findActiveWithDeadlineUpTo(Instant instant);
}
