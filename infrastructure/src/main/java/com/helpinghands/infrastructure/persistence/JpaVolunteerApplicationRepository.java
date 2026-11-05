package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.ApplicationStatus;
import com.helpinghands.core.domain.VolunteerApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaVolunteerApplicationRepository extends JpaRepository<VolunteerApplication, Long> {

    boolean existsByOpportunityIdAndVolunteerEmail(Long opportunityId, String volunteerEmail);

    @Query("""
            SELECT a FROM VolunteerApplication a
            WHERE a.opportunity.id = :opportunityId
              AND (:status IS NULL OR a.status = :status)
            """)
    Page<VolunteerApplication> findByOpportunity(@Param("opportunityId") Long opportunityId,
                                                 @Param("status") ApplicationStatus status,
                                                 Pageable pageable);
}
