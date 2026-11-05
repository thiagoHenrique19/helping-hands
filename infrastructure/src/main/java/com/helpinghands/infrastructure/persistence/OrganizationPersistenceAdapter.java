package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.port.out.OrganizationRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class OrganizationPersistenceAdapter implements OrganizationRepository {

    private final JpaOrganizationRepository jpaRepository;

    OrganizationPersistenceAdapter(JpaOrganizationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Organization save(Organization organization) {
        return jpaRepository.save(organization);
    }

    @Override
    public Optional<Organization> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
