package com.helpinghands.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.AbstractPostgresIntegrationTest;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.Role;
import com.helpinghands.core.domain.User;
import com.helpinghands.core.port.out.OrganizationRepository;
import com.helpinghands.core.port.out.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class UserPersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationRepository organizationRepository;

    @Test
    void shouldPersistAndFindByEmail() {
        String email = UUID.randomUUID() + "@mail.com";
        userRepository.save(User.register(email, "hash", Role.VOLUNTEER));

        User found = userRepository.findByEmail(email).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getRole()).isEqualTo(Role.VOLUNTEER);
        assertThat(found.isActive()).isTrue();
        assertThat(userRepository.existsByEmail(email)).isTrue();
        assertThat(userRepository.count()).isPositive();
    }

    @Test
    void shouldEnforceUniqueEmail() {
        String email = UUID.randomUUID() + "@mail.com";
        userRepository.save(User.register(email, "hash", Role.VOLUNTEER));

        assertThatThrownBy(() -> userRepository.save(User.register(email, "hash", Role.VOLUNTEER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPersistOrganizationOwnership() {
        Organization organization = organizationRepository.save(
                Organization.register("Org", UUID.randomUUID() + "@org.org", "d"));
        User user = userRepository.save(User.register(UUID.randomUUID() + "@mail.com", "hash", Role.ORGANIZATION));
        user.assignOrganization(organization.getId());
        userRepository.save(user);

        User found = userRepository.findById(user.getId()).orElseThrow();

        assertThat(found.ownsOrganization(organization.getId())).isTrue();
    }
}
