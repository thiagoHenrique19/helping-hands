package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.OpportunitySearchCriteria;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class VolunteerOpportunityPersistenceAdapter implements VolunteerOpportunityRepository {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "title", "startsAt", "endsAt", "slots", "createdAt", "status");

    private final JpaVolunteerOpportunityRepository jpaRepository;
    private final EntityManager entityManager;

    VolunteerOpportunityPersistenceAdapter(JpaVolunteerOpportunityRepository jpaRepository,
                                           EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public VolunteerOpportunity save(VolunteerOpportunity opportunity) {
        return jpaRepository.save(opportunity);
    }

    @Override
    public Optional<VolunteerOpportunity> findById(Long id) {
        return jpaRepository.findById(id);
    }

    // A locking JPQL query would hand back the already-managed instance with stale counters,
    // so the entity is refreshed under the lock instead.
    @Override
    public Optional<VolunteerOpportunity> findByIdForUpdate(Long id) {
        var opportunity = entityManager.find(VolunteerOpportunity.class, id);
        if (opportunity != null) {
            entityManager.refresh(opportunity, LockModeType.PESSIMISTIC_WRITE);
        }
        return Optional.ofNullable(opportunity);
    }

    @Override
    public PageResult<VolunteerOpportunity> search(OpportunitySearchCriteria criteria, Instant now,
                                                   PageQuery pageQuery) {
        var pageable = PageTranslator.toPageable(pageQuery, SORTABLE_FIELDS);
        return PageTranslator.toPageResult(jpaRepository.findAll(specification(criteria, now), pageable));
    }

    private Specification<VolunteerOpportunity> specification(OpportunitySearchCriteria criteria, Instant now) {
        Specification<VolunteerOpportunity> specification = Specification.where(null);
        if (criteria.organizationId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("organization").get("id"), criteria.organizationId()));
        }
        if (criteria.status() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), criteria.status()));
        }
        if (criteria.remote() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("remote"), criteria.remote()));
        }
        if (Boolean.TRUE.equals(criteria.upcoming())) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThan(root.get("startsAt"), now));
        }
        return specification;
    }
}
