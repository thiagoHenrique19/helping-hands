package com.helpinghands.application.mapper;

import com.helpinghands.core.port.in.dto.CampaignRequest;
import com.helpinghands.core.port.in.dto.CampaignResponse;
import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.Organization;
import org.springframework.stereotype.Component;

@Component
public class CampaignMapper {

    public Campaign toEntity(Organization organization, CampaignRequest request) {
        return Campaign.create(organization, request.title(), request.description(), request.goalAmount());
    }

    public CampaignResponse toResponse(Campaign campaign) {
        return new CampaignResponse(campaign.getId(), campaign.getOrganization().getId(), campaign.getTitle(),
                campaign.getDescription(), campaign.getGoalAmount(), campaign.getRaisedAmount(),
                campaign.progressPercentage(), campaign.getStatus());
    }
}
