package com.helpinghands.infrastructure.security;

import com.helpinghands.core.domain.User;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.UserRepository;
import com.helpinghands.core.port.out.VolunteerApplicationRepository;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ownership checks used from {@code @PreAuthorize} expressions as {@code @access}. Administrators are
 * handled with {@code hasRole('ADMIN')} in the expressions themselves.
 */
@Component("access")
public class AccessPolicy {

    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final VolunteerOpportunityRepository opportunityRepository;
    private final VolunteerApplicationRepository applicationRepository;

    public AccessPolicy(UserRepository userRepository,
                        CampaignRepository campaignRepository,
                        VolunteerOpportunityRepository opportunityRepository,
                        VolunteerApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.opportunityRepository = opportunityRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public boolean ownsOrganization(Authentication authentication, Long organizationId) {
        return organizationId != null && currentUser(authentication)
                .map(user -> user.ownsOrganization(organizationId))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean ownsCampaign(Authentication authentication, Long campaignId) {
        return campaignRepository.findById(campaignId)
                .map(campaign -> ownsOrganization(authentication, campaign.getOrganization().getId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean ownsOpportunity(Authentication authentication, Long opportunityId) {
        return opportunityRepository.findById(opportunityId)
                .map(opportunity -> ownsOrganization(authentication, opportunity.getOrganization().getId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean ownsApplication(Authentication authentication, Long applicationId) {
        return applicationRepository.findById(applicationId)
                .map(application -> ownsOrganization(authentication,
                        application.getOpportunity().getOrganization().getId()))
                .orElse(false);
    }

    private Optional<User> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        try {
            return userRepository.findById(Long.valueOf(authentication.getName()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
