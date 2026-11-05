package com.helpinghands.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.core.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void shouldRegisterActiveUserWithNormalizedEmail() {
        User user = User.register(" Jane@Mail.com ", "hash", Role.VOLUNTEER);

        assertThat(user.getEmail()).isEqualTo("jane@mail.com");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getRole()).isEqualTo(Role.VOLUNTEER);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getOrganizationId()).isNull();
    }

    @Test
    void shouldAssignOrganizationToOrganizationUser() {
        User user = User.register("ngo@mail.com", "hash", Role.ORGANIZATION);

        user.assignOrganization(10L);

        assertThat(user.getOrganizationId()).isEqualTo(10L);
        assertThat(user.ownsOrganization(10L)).isTrue();
        assertThat(user.ownsOrganization(11L)).isFalse();
    }

    @Test
    void shouldRejectAssigningSecondOrganization() {
        User user = User.register("ngo@mail.com", "hash", Role.ORGANIZATION);
        user.assignOrganization(10L);

        assertThatThrownBy(() -> user.assignOrganization(11L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already");
    }

    @Test
    void shouldRejectAssigningOrganizationToNonOrganizationUsers() {
        User volunteer = User.register("vol@mail.com", "hash", Role.VOLUNTEER);
        User admin = User.register("admin@mail.com", "hash", Role.ADMIN);

        assertThatThrownBy(() -> volunteer.assignOrganization(1L)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> admin.assignOrganization(1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldNotOwnOrganizationWhenDeactivated() {
        User user = User.register("ngo@mail.com", "hash", Role.ORGANIZATION);
        user.assignOrganization(10L);

        user.deactivate();

        assertThat(user.isActive()).isFalse();
        assertThat(user.ownsOrganization(10L)).isFalse();
    }
}
