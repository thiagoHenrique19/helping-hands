package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.ApplicationStatus;
import com.helpinghands.core.domain.VolunteerApplication;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.out.VolunteerApplicationRepository;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
class VolunteerApplicationPersistenceAdapter implements VolunteerApplicationRepository {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "createdAt", "volunteerName", "status");

    private final JpaVolunteerApplicationRepository jpaRepository;

    VolunteerApplicationPersistenceAdapter(JpaVolunteerApplicationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public VolunteerApplication save(VolunteerApplication application) {
        return jpaRepository.save(application);
    }

    @Override
    public Optional<VolunteerApplication> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsByOpportunityIdAndVolunteerEmail(Long opportunityId, String volunteerEmail) {
        return jpaRepository.existsByOpportunityIdAndVolunteerEmail(opportunityId, volunteerEmail);
    }

    @Override
    public PageResult<VolunteerApplication> findByOpportunityId(Long opportunityId, ApplicationStatus status,
                                                                PageQuery pageQuery) {
        var pageable = PageTranslator.toPageable(pageQuery, SORTABLE_FIELDS);
        return PageTranslator.toPageResult(jpaRepository.findByOpportunity(opportunityId, status, pageable));
    }
}
