package com.helpinghands.core.domain;

import com.helpinghands.core.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

@Entity
public class VolunteerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id")
    private VolunteerOpportunity opportunity;

    private String volunteerName;
    private String volunteerEmail;
    private String volunteerPhone;

    @Column(length = 2000)
    private String motivation;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    private Instant createdAt = Instant.now();

    protected VolunteerApplication() {
    }

    private VolunteerApplication(VolunteerOpportunity opportunity, String volunteerName, String volunteerEmail,
                                 String volunteerPhone, String motivation) {
        this.opportunity = opportunity;
        this.volunteerName = volunteerName;
        this.volunteerEmail = Organization.normalizeEmail(volunteerEmail);
        this.volunteerPhone = volunteerPhone;
        this.motivation = motivation;
    }

    public static VolunteerApplication apply(VolunteerOpportunity opportunity, String volunteerName,
                                             String volunteerEmail, String volunteerPhone, String motivation) {
        opportunity.assertAcceptsApplications();
        return new VolunteerApplication(opportunity, volunteerName, volunteerEmail, volunteerPhone, motivation);
    }

    public void approve() {
        requireStatus(ApplicationStatus.PENDING, "Only pending applications can be approved");
        opportunity.fillSlot();
        status = ApplicationStatus.APPROVED;
    }

    public void reject() {
        requireStatus(ApplicationStatus.PENDING, "Only pending applications can be rejected");
        status = ApplicationStatus.REJECTED;
    }

    public void withdraw() {
        if (status != ApplicationStatus.PENDING && status != ApplicationStatus.APPROVED) {
            throw new BusinessRuleException("Only pending or approved applications can be withdrawn");
        }
        if (status == ApplicationStatus.APPROVED) {
            opportunity.releaseSlot();
        }
        status = ApplicationStatus.WITHDRAWN;
    }

    private void requireStatus(ApplicationStatus expected, String message) {
        if (status != expected) {
            throw new BusinessRuleException(message);
        }
    }

    public Long getId() {
        return id;
    }

    public VolunteerOpportunity getOpportunity() {
        return opportunity;
    }

    public String getVolunteerName() {
        return volunteerName;
    }

    public String getVolunteerEmail() {
        return volunteerEmail;
    }

    public String getVolunteerPhone() {
        return volunteerPhone;
    }

    public String getMotivation() {
        return motivation;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
