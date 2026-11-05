package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.CampaignSearchCriteria;
import com.helpinghands.core.port.out.CampaignRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
class CampaignPersistenceAdapter implements CampaignRepository {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "title", "goalAmount", "raisedAmount", "endsAt", "createdAt", "status", "category");

    private final JpaCampaignRepository jpaRepository;

    CampaignPersistenceAdapter(JpaCampaignRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Campaign save(Campaign campaign) {
        return jpaRepository.save(campaign);
    }

    @Override
    public Optional<Campaign> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Campaign> findByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id);
    }

    @Override
    public PageResult<Campaign> search(CampaignSearchCriteria criteria, PageQuery pageQuery) {
        var pageable = PageTranslator.toPageable(pageQuery, SORTABLE_FIELDS);
        return PageTranslator.toPageResult(jpaRepository.findAll(CampaignSpecifications.from(criteria), pageable));
    }

    @Override
    public List<Campaign> findActiveWithDeadlineUpTo(Instant instant) {
        return jpaRepository.findByStatusAndEndsAtLessThanEqual(CampaignStatus.ACTIVE, instant);
    }
}
