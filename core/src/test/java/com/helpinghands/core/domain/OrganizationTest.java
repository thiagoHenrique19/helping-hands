package com.helpinghands.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.core.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class OrganizationTest {

    private Organization organization() {
        return Organization.register("Hope NGO", "Hope@NGO.org ", "Food aid", "+55 11 99999-0000", "https://hope.org");
    }

    @Test
    void shouldRegisterUnverifiedWithNormalizedEmail() {
        Organization organization = organization();

        assertThat(organization.isVerified()).isFalse();
        assertThat(organization.getEmail()).isEqualTo("hope@ngo.org");
        assertThat(organization.getPhone()).isEqualTo("+55 11 99999-0000");
        assertThat(organization.getWebsite()).isEqualTo("https://hope.org");
    }

    @Test
    void shouldVerifyOrganization() {
        Organization organization = organization();

        organization.verify();

        assertThat(organization.isVerified()).isTrue();
    }

    @Test
    void shouldRejectVerifyingTwice() {
        Organization organization = organization();
        organization.verify();

        assertThatThrownBy(organization::verify)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already verified");
    }

    @Test
    void shouldUpdateDetails() {
        Organization organization = organization();

        organization.update("New Name", "New@ngo.org", "New desc", null, "https://new.org");

        assertThat(organization.getName()).isEqualTo("New Name");
        assertThat(organization.getEmail()).isEqualTo("new@ngo.org");
        assertThat(organization.getPhone()).isNull();
        assertThat(organization.getWebsite()).isEqualTo("https://new.org");
    }
}
