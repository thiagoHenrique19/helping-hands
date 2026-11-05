package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.VolunteerOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface JpaVolunteerOpportunityRepository
        extends JpaRepository<VolunteerOpportunity, Long>, JpaSpecificationExecutor<VolunteerOpportunity> {
}
