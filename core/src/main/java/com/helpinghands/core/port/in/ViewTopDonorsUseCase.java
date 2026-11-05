package com.helpinghands.core.port.in;

import com.helpinghands.core.port.in.dto.TopDonorResponse;
import java.util.List;

public interface ViewTopDonorsUseCase {

    int MAX_LIMIT = 50;

    List<TopDonorResponse> topDonors(Long campaignId, int limit);
}
