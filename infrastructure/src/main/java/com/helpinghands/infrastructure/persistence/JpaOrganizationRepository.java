package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaOrganizationRepository extends JpaRepository<Organization, Long> {
}
