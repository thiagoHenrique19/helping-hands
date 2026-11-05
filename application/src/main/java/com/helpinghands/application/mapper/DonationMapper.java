package com.helpinghands.application.mapper;

import com.helpinghands.core.domain.Donation;
import com.helpinghands.core.port.in.dto.DonationResponse;
import com.helpinghands.core.port.in.dto.PublicDonationResponse;
import org.springframework.stereotype.Component;

@Component
public class DonationMapper {

    public DonationResponse toResponse(Donation donation) {
        return new DonationResponse(donation.getId(), donation.getCampaign().getId(), donation.getDonorName(),
                donation.getAmount(), donation.getCreatedAt(), donation.getReceiptCode(), donation.isAnonymous(),
                donation.getMessage(), donation.getPaymentMethod());
    }

    public PublicDonationResponse toPublicResponse(Donation donation) {
        return new PublicDonationResponse(donation.getId(), donation.publicDonorName(), donation.getAmount(),
                donation.getMessage(), donation.getCreatedAt());
    }
}
