package com.helpinghands.application.service;

import com.helpinghands.core.port.in.ManageCampaignsUseCase;
import com.helpinghands.core.port.in.dto.CampaignRequest;
import com.helpinghands.core.port.in.dto.CampaignResponse;
import com.helpinghands.application.mapper.CampaignMapper;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.OrganizationRepository;
import java.util.List;
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
        return campaignRepository.findById(id)
                .map(campaignMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResponse> listActive() {
        return campaignRepository.findByStatus(CampaignStatus.ACTIVE).stream()
                .map(campaignMapper::toResponse)
                .toList();
    }
}
