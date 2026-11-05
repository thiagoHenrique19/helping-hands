package com.helpinghands.application.service;

import com.helpinghands.application.mapper.CampaignMapper;
import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.ManageCampaignsUseCase;
import com.helpinghands.core.port.in.dto.CampaignRequest;
import com.helpinghands.core.port.in.dto.CampaignResponse;
import com.helpinghands.core.port.in.dto.CampaignSearchCriteria;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignService implements ManageCampaignsUseCase {

    private final CampaignRepository campaignRepository;
    private final OrganizationRepository organizationRepository;
    private final CampaignMapper campaignMapper;

    public CampaignService(CampaignRepository campaignRepository,
                           OrganizationRepository organizationRepository,
                           CampaignMapper campaignMapper) {
        this.campaignRepository = campaignRepository;
        this.organizationRepository = organizationRepository;
        this.campaignMapper = campaignMapper;
    }

    @Override
    @Transactional
    public CampaignResponse create(Long organizationId, CampaignRequest request) {
        var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
        var saved = campaignRepository.save(campaignMapper.toEntity(organization, request));
        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse findById(Long id) {
        return campaignMapper.toResponse(load(id));
    }

    @Override
    @Transactional
    public CampaignResponse update(Long id, CampaignRequest request) {
        var campaign = loadForUpdate(id);
        campaign.update(request.title(), request.description(), request.goalAmount(), request.endsAt(),
                request.category());
        return campaignMapper.toResponse(campaignRepository.save(campaign));
    }

    @Override
    @Transactional
    public CampaignResponse cancel(Long id) {
        var campaign = loadForUpdate(id);
        campaign.cancel();
        return campaignMapper.toResponse(campaignRepository.save(campaign));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CampaignResponse> search(CampaignSearchCriteria criteria, PageQuery pageQuery) {
        return campaignRepository.search(criteria, pageQuery).map(campaignMapper::toResponse);
    }

    private Campaign load(Long id) {
        return campaignRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
    }

    private Campaign loadForUpdate(Long id) {
        return campaignRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
    }
}
