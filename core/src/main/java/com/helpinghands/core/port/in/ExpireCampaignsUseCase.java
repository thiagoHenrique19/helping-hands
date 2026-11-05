package com.helpinghands.core.port.in;

public interface ExpireCampaignsUseCase {

    /**
     * Marks every active campaign whose deadline has passed as expired.
     *
     * @return the number of campaigns that were expired
     */
    int expireOverdueCampaigns();
}
