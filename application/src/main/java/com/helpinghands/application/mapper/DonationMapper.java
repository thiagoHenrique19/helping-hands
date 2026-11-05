package com.helpinghands.application.mapper;

import com.helpinghands.core.port.in.dto.DonationResponse;
import com.helpinghands.core.domain.Donation;
import org.springframework.stereotype.Component;

@Component
public class DonationMapper {

    public DonationResponse toResponse(Donation donation) {
        return new DonationResponse(donation.getId(), donation.getCampaign().getId(),
                donation.getDonorName(), donation.getAmount(), donation.getCreatedAt());
    }
}
