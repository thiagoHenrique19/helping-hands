package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.Donation;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.TopDonorResponse;
import com.helpinghands.core.port.out.DonationRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
class DonationPersistenceAdapter implements DonationRepository {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "amount", "createdAt");

    private final JpaDonationRepository jpaRepository;

    DonationPersistenceAdapter(JpaDonationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Donation save(Donation donation) {
        return jpaRepository.save(donation);
    }

    @Override
    public PageResult<Donation> findByCampaignId(Long campaignId, PageQuery pageQuery) {
        var pageable = PageTranslator.toPageable(pageQuery, SORTABLE_FIELDS);
        return PageTranslator.toPageResult(jpaRepository.findByCampaignId(campaignId, pageable));
    }

    @Override
    public List<TopDonorResponse> findTopDonors(Long campaignId, int limit) {
        return jpaRepository.findTopDonors(campaignId, limit).stream()
                .map(row -> new TopDonorResponse(row.getDonorName(), row.getTotalAmount(), row.getDonationsCount()))
                .toList();
    }
}
