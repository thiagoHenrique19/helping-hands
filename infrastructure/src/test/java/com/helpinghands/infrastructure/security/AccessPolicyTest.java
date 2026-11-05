package com.helpinghands.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.Role;
import com.helpinghands.core.domain.User;
import com.helpinghands.core.domain.VolunteerApplication;
import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.UserRepository;
import com.helpinghands.core.port.out.VolunteerApplicationRepository;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccessPolicyTest {

    @Mock private UserRepository userRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private VolunteerOpportunityRepository opportunityRepository;
    @Mock private VolunteerApplicationRepository applicationRepository;

    private AccessPolicy policy;
    private final Authentication owner = new TestingAuthenticationToken("5", null, "ROLE_ORGANIZATION");
    private Organization organization;

    @BeforeEach
    void setUp() {
        policy = new AccessPolicy(userRepository, campaignRepository, opportunityRepository, applicationRepository);
        organization = Organization.register("Org", "o@org.org", "d");
        ReflectionTestUtils.setField(organization, "id", 10L);
    }

    private void ownerOfOrganization(Long organizationId) {
        User user = User.register("ngo@mail.com", "hash", Role.ORGANIZATION);
        user.assignOrganization(organizationId);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
    }

    @Test
    void shouldRecognizeOrganizationOwner() {
        ownerOfOrganization(10L);

        assertThat(policy.ownsOrganization(owner, 10L)).isTrue();
        assertThat(policy.ownsOrganization(owner, 11L)).isFalse();
    }

    @Test
    void shouldDenyWhenUserDoesNotExistOrSubjectIsNotNumeric() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThat(policy.ownsOrganization(owner, 10L)).isFalse();
        assertThat(policy.ownsOrganization(new TestingAuthenticationToken("not-a-number", null), 10L)).isFalse();
        assertThat(policy.ownsOrganization(null, 10L)).isFalse();
        assertThat(policy.ownsOrganization(owner, null)).isFalse();
    }

    @Test
    void shouldDenyDeactivatedOwner() {
        User user = User.register("ngo@mail.com", "hash", Role.ORGANIZATION);
        user.assignOrganization(10L);
        user.deactivate();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        assertThat(policy.ownsOrganization(owner, 10L)).isFalse();
    }

    @Test
    void shouldResolveOwnershipThroughCampaign() {
        ownerOfOrganization(10L);
        when(campaignRepository.findById(1L))
                .thenReturn(Optional.of(Campaign.create(organization, "T", "d", BigDecimal.TEN)));
        when(campaignRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(policy.ownsCampaign(owner, 1L)).isTrue();
        assertThat(policy.ownsCampaign(owner, 2L)).isFalse();
    }

    @Test
    void shouldResolveOwnershipThroughOpportunityAndApplication() {
        ownerOfOrganization(10L);
        Instant startsAt = Instant.now().plus(2, ChronoUnit.DAYS);
        VolunteerOpportunity opportunity = VolunteerOpportunity.create(organization, "T", "d", "L", false, startsAt,
                startsAt.plus(1, ChronoUnit.HOURS), 2);
        when(opportunityRepository.findById(1L)).thenReturn(Optional.of(opportunity));
        when(applicationRepository.findById(2L)).thenReturn(
                Optional.of(VolunteerApplication.apply(opportunity, "V", "v@mail.com", null, null)));

        assertThat(policy.ownsOpportunity(owner, 1L)).isTrue();
        assertThat(policy.ownsApplication(owner, 2L)).isTrue();
    }

    @Test
    void shouldDenyOwnershipOfOtherOrganizationsResources() {
        ownerOfOrganization(99L);
        when(campaignRepository.findById(1L))
                .thenReturn(Optional.of(Campaign.create(organization, "T", "d", BigDecimal.TEN)));

        assertThat(policy.ownsCampaign(owner, 1L)).isFalse();
    }
}
