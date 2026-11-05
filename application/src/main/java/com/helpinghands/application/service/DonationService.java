package com.helpinghands.application.service;

import com.helpinghands.core.port.in.MakeDonationUseCase;
import com.helpinghands.core.port.in.dto.DonationRequest;
import com.helpinghands.core.port.in.dto.DonationResponse;
import com.helpinghands.application.mapper.DonationMapper;
import com.helpinghands.core.domain.Donation;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.DonationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonationService implements MakeDonationUseCase {

    private final CampaignRepository campaignRepository;
    private final DonationRepository donationRepository;
    private final DonationMapper donationMapper;

    public DonationService(CampaignRepository campaignRepository,
                           DonationRepository donationRepository,
                           DonationMapper donationMapper) {
        this.campaignRepository = campaignRepository;
        this.donationRepository = donationRepository;
        this.donationMapper = donationMapper;
    }

    @Override
    @Transactional
    public DonationResponse donate(Long campaignId, DonationRequest request) {
        var campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
        campaign.receiveDonation(request.amount());
        var donation = Donation.make(campaign, request.donorName(), request.donorEmail(), request.amount());
        return donationMapper.toResponse(donationRepository.save(donation));
    }
}
