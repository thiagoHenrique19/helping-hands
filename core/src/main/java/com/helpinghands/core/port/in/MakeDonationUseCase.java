package com.helpinghands.core.port.in;

import com.helpinghands.core.port.in.dto.DonationRequest;
import com.helpinghands.core.port.in.dto.DonationResponse;

public interface MakeDonationUseCase {

    DonationResponse donate(Long campaignId, DonationRequest request);
}
