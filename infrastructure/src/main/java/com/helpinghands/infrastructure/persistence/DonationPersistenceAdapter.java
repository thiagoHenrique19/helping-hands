package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.Donation;
import com.helpinghands.core.port.out.DonationRepository;
import org.springframework.stereotype.Repository;

@Repository
class DonationPersistenceAdapter implements DonationRepository {

    private final JpaDonationRepository jpaRepository;

    DonationPersistenceAdapter(JpaDonationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Donation save(Donation donation) {
        return jpaRepository.save(donation);
    }
}
