package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.Donation;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaDonationRepository extends JpaRepository<Donation, Long> {
}
