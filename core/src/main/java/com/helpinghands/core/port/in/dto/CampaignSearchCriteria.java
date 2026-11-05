package com.helpinghands.core.port.in.dto;

import com.helpinghands.core.domain.CampaignCategory;
import com.helpinghands.core.domain.CampaignStatus;

public record CampaignSearchCriteria(
        Long organizationId,
        CampaignStatus status,
        CampaignCategory category,
        String title) {

    public static CampaignSearchCriteria none() {
        return new CampaignSearchCriteria(null, null, null, null);
    }
}
