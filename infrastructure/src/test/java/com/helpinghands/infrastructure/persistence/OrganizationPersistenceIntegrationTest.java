package com.helpinghands.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.AbstractPostgresIntegrationTest;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.port.out.OrganizationRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class OrganizationPersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private OrganizationRepository organizationRepository;

    @Test
    void shouldPersistContactDetailsAndVerification() {
        Organization saved = organizationRepository.save(Organization.register("Contact Org",
                UUID.randomUUID() + "@org.org", "d", "123", "https://org.org"));
        saved.verify();
        organizationRepository.save(saved);

        Organization found = organizationRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getPhone()).isEqualTo("123");
        assertThat(found.getWebsite()).isEqualTo("https://org.org");
        assertThat(found.isVerified()).isTrue();
    }

    @Test
    void shouldEnforceUniqueEmailInDatabase() {
        String email = UUID.randomUUID() + "@dup.org";
        organizationRepository.save(Organization.register("First", email, "d"));

        assertThat(organizationRepository.existsByEmail(email)).isTrue();
        assertThatThrownBy(() -> organizationRepository.save(Organization.register("Second", email, "d")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldCheckEmailOwnershipExcludingSelf() {
        String email = UUID.randomUUID() + "@own.org";
        Organization saved = organizationRepository.save(Organization.register("Owner", email, "d"));

        assertThat(organizationRepository.existsByEmailAndIdNot(email, saved.getId())).isFalse();
        assertThat(organizationRepository.existsByEmailAndIdNot(email, saved.getId() + 1000)).isTrue();
    }

    @Test
    void shouldSearchByNameIgnoringCaseWithPagination() {
        String token = "zz" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 3; i++) {
            organizationRepository.save(Organization.register("Org " + token.toUpperCase() + " " + i,
                    UUID.randomUUID() + "@s.org", "d"));
        }

        var page = organizationRepository.searchByName(token, PageQuery.of(0, 2));

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }
}
