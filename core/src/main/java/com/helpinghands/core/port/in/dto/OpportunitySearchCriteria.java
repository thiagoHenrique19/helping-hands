package com.helpinghands.core.port.in.dto;

import com.helpinghands.core.domain.OpportunityStatus;

public record OpportunitySearchCriteria(
        Long organizationId,
        OpportunityStatus status,
        Boolean remote,
        Boolean upcoming) {

    public static OpportunitySearchCriteria none() {
        return new OpportunitySearchCriteria(null, null, null, null);
    }
}
