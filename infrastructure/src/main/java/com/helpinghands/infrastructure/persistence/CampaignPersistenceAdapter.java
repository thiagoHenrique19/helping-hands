package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.port.out.CampaignRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class CampaignPersistenceAdapter implements CampaignRepository {

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
    public List<Campaign> findByStatus(CampaignStatus status) {
        return jpaRepository.findByStatus(status);
    }
}
