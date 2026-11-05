package com.helpinghands.application.service;

import com.helpinghands.core.port.in.RegisterOrganizationUseCase;
import com.helpinghands.core.port.in.dto.OrganizationRequest;
import com.helpinghands.core.port.in.dto.OrganizationResponse;
import com.helpinghands.application.mapper.OrganizationMapper;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.port.out.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService implements RegisterOrganizationUseCase {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    public OrganizationService(OrganizationRepository organizationRepository, OrganizationMapper organizationMapper) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
    }

    @Override
    @Transactional
    public OrganizationResponse register(OrganizationRequest request) {
        var saved = organizationRepository.save(organizationMapper.toEntity(request));
        return organizationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse findById(Long id) {
        return organizationRepository.findById(id)
                .map(organizationMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", id));
    }
}
