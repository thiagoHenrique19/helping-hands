package com.helpinghands.application.service;

import com.helpinghands.application.mapper.DonationMapper;
import com.helpinghands.core.exception.InvalidRequestException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.ListDonationsUseCase;
import com.helpinghands.core.port.in.ViewTopDonorsUseCase;
import com.helpinghands.core.port.in.dto.PublicDonationResponse;
import com.helpinghands.core.port.in.dto.TopDonorResponse;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.DonationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DonationQueryService implements ListDonationsUseCase, ViewTopDonorsUseCase {

    private final CampaignRepository campaignRepository;
    private final DonationRepository donationRepository;
    private final DonationMapper donationMapper;

    public DonationQueryService(CampaignRepository campaignRepository,
                                DonationRepository donationRepository,
                                DonationMapper donationMapper) {
        this.campaignRepository = campaignRepository;
        this.donationRepository = donationRepository;
        this.donationMapper = donationMapper;
    }

    @Override
    public PageResult<PublicDonationResponse> listByCampaign(Long campaignId, PageQuery pageQuery) {
        requireCampaign(campaignId);
        return donationRepository.findByCampaignId(campaignId, pageQuery).map(donationMapper::toPublicResponse);
    }

    @Override
    public List<TopDonorResponse> topDonors(Long campaignId, int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidRequestException("Limit must be between 1 and " + MAX_LIMIT);
        }
        requireCampaign(campaignId);
        return donationRepository.findTopDonors(campaignId, limit);
    }

    private void requireCampaign(Long campaignId) {
        campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
    }
}
