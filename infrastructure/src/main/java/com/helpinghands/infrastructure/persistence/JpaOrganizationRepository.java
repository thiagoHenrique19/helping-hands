package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.domain.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaOrganizationRepository extends JpaRepository<Organization, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Page<Organization> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
