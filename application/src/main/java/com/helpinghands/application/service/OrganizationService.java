package com.helpinghands.application.service;

import com.helpinghands.application.mapper.OrganizationMapper;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.exception.ConflictException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.ListOrganizationsUseCase;
import com.helpinghands.core.port.in.RegisterOrganizationUseCase;
import com.helpinghands.core.port.in.UpdateOrganizationUseCase;
import com.helpinghands.core.port.in.VerifyOrganizationUseCase;
import com.helpinghands.core.port.in.dto.OrganizationRequest;
import com.helpinghands.core.port.in.dto.OrganizationResponse;
import com.helpinghands.core.port.out.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService implements RegisterOrganizationUseCase, UpdateOrganizationUseCase,
        VerifyOrganizationUseCase, ListOrganizationsUseCase {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    public OrganizationService(OrganizationRepository organizationRepository, OrganizationMapper organizationMapper) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
    }

    @Override
    @Transactional
    public OrganizationResponse register(OrganizationRequest request) {
        String email = Organization.normalizeEmail(request.email());
        if (organizationRepository.existsByEmail(email)) {
            throw new ConflictException("An organization with email " + email + " already exists");
        }
        var saved = organizationRepository.save(organizationMapper.toEntity(request));
        return organizationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse findById(Long id) {
        return organizationMapper.toResponse(load(id));
    }

    @Override
    @Transactional
    public OrganizationResponse update(Long id, OrganizationRequest request) {
        var organization = load(id);
        String email = Organization.normalizeEmail(request.email());
        if (organizationRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("An organization with email " + email + " already exists");
        }
        organization.update(request.name(), email, request.description(), request.phone(), request.website());
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    @Override
    @Transactional
    public OrganizationResponse verify(Long id) {
        var organization = load(id);
        organization.verify();
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OrganizationResponse> search(String name, PageQuery pageQuery) {
        return organizationRepository.searchByName(name, pageQuery).map(organizationMapper::toResponse);
    }

    private Organization load(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", id));
    }
}
