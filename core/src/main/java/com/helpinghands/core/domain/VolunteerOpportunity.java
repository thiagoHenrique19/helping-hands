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
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
public class VolunteerOpportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    private String title;

    @Column(length = 2000)
    private String description;

    private String location;
    private boolean remote;
    private Instant startsAt;
    private Instant endsAt;
    private int slots;
    private int filledSlots;

    @Enumerated(EnumType.STRING)
    private OpportunityStatus status = OpportunityStatus.OPEN;

    private Instant createdAt = Instant.now();

    @Version
    private Long version;

    protected VolunteerOpportunity() {
    }

    private VolunteerOpportunity(Organization organization, String title, String description, String location,
                                 boolean remote, Instant startsAt, Instant endsAt, int slots) {
        this.organization = organization;
        this.title = title;
        this.description = description;
        this.location = location;
        this.remote = remote;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.slots = slots;
    }

    public static VolunteerOpportunity create(Organization organization, String title, String description,
                                              String location, boolean remote, Instant startsAt, Instant endsAt,
                                              int slots) {
        if (slots < 1) {
            throw new BusinessRuleException("Slots must be at least 1");
        }
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new BusinessRuleException("End date must be after the start date");
        }
        if (!startsAt.isAfter(Instant.now())) {
            throw new BusinessRuleException("Start date must be in the future");
        }
        if (!remote && (location == null || location.isBlank())) {
            throw new BusinessRuleException("Location is required for on-site opportunities");
        }
        return new VolunteerOpportunity(organization, title, description, location, remote, startsAt, endsAt, slots);
    }

    public void assertAcceptsApplications() {
        if (status == OpportunityStatus.CLOSED || status == OpportunityStatus.CANCELLED) {
            throw new BusinessRuleException("Opportunity is " + status.name().toLowerCase()
                    + " and no longer accepts applications");
        }
    }

    public void fillSlot() {
        if (status == OpportunityStatus.CLOSED || status == OpportunityStatus.CANCELLED) {
            throw new BusinessRuleException("Opportunity is " + status.name().toLowerCase());
        }
        if (filledSlots >= slots) {
            throw new BusinessRuleException("No slots available");
        }
        filledSlots++;
        if (filledSlots == slots) {
            status = OpportunityStatus.FULL;
        }
    }

    public void releaseSlot() {
        if (filledSlots > 0) {
            filledSlots--;
        }
        if (status == OpportunityStatus.FULL) {
            status = OpportunityStatus.OPEN;
        }
    }

    public void close() {
        if (status != OpportunityStatus.OPEN && status != OpportunityStatus.FULL) {
            throw new BusinessRuleException("Only open or full opportunities can be closed");
        }
        status = OpportunityStatus.CLOSED;
    }

    public void cancel() {
        if (status == OpportunityStatus.CANCELLED) {
            throw new BusinessRuleException("Opportunity is already cancelled");
        }
        status = OpportunityStatus.CANCELLED;
    }

    public int availableSlots() {
        return slots - filledSlots;
    }

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public boolean isRemote() {
        return remote;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public int getSlots() {
        return slots;
    }

    public int getFilledSlots() {
        return filledSlots;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
