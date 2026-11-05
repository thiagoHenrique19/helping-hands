package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.OpportunitySearchCriteria;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
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

    VolunteerOpportunityPersistenceAdapter(JpaVolunteerOpportunityRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public VolunteerOpportunity save(VolunteerOpportunity opportunity) {
        return jpaRepository.save(opportunity);
    }

    @Override
    public Optional<VolunteerOpportunity> findById(Long id) {
        return jpaRepository.findById(id);
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
