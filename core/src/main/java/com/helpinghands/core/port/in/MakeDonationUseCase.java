package com.helpinghands.core.port.in;

import com.helpinghands.core.port.in.dto.DonationRequest;
import com.helpinghands.core.port.in.dto.DonationResponse;

public interface MakeDonationUseCase {

    /** @param idempotencyKey optional; repeating a key with the same request returns the first response */
    DonationResponse donate(Long campaignId, DonationRequest request, String idempotencyKey);
}
