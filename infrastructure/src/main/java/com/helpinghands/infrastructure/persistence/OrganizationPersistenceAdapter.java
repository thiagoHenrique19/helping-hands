package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.out.OrganizationRepository;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
class OrganizationPersistenceAdapter implements OrganizationRepository {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "email");

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

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return jpaRepository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public PageResult<Organization> searchByName(String name, PageQuery pageQuery) {
        var pageable = PageTranslator.toPageable(pageQuery, SORTABLE_FIELDS);
        return PageTranslator.toPageResult(
                jpaRepository.findByNameContainingIgnoreCase(name == null ? "" : name.trim(), pageable));
    }
}
