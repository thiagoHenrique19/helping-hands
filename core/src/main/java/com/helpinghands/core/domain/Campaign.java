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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    private String title;
    private String description;

    @Column(precision = 19, scale = 2)
    private BigDecimal goalAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal raisedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private CampaignStatus status = CampaignStatus.ACTIVE;

    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    private CampaignCategory category = CampaignCategory.OTHER;

    private Instant createdAt = Instant.now();

    @Version
    private Long version;

    protected Campaign() {
    }

    private Campaign(Organization organization, String title, String description, BigDecimal goalAmount,
                     Instant endsAt, CampaignCategory category) {
        this.organization = organization;
        this.title = title;
        this.description = description;
        this.goalAmount = goalAmount;
        this.endsAt = endsAt;
        this.category = category == null ? CampaignCategory.OTHER : category;
    }

    public static Campaign create(Organization organization, String title, String description, BigDecimal goalAmount) {
        return create(organization, title, description, goalAmount, null, CampaignCategory.OTHER);
    }

    public static Campaign create(Organization organization, String title, String description, BigDecimal goalAmount,
                                  Instant endsAt, CampaignCategory category) {
        validateGoal(goalAmount);
        validateDeadline(endsAt);
        return new Campaign(organization, title, description, goalAmount, endsAt, category);
    }

    public void update(String title, String description, BigDecimal goalAmount, Instant endsAt,
                       CampaignCategory category) {
        if (status != CampaignStatus.ACTIVE) {
            throw new BusinessRuleException("Only active campaigns can be updated");
        }
        validateGoal(goalAmount);
        if (goalAmount.compareTo(raisedAmount) <= 0) {
            throw new BusinessRuleException("Goal amount must be greater than the amount already raised");
        }
        validateDeadline(endsAt);
        this.title = title;
        this.description = description;
        this.goalAmount = goalAmount;
        this.endsAt = endsAt;
        this.category = category == null ? CampaignCategory.OTHER : category;
    }

    public boolean expireIfOverdue(Instant now) {
        if (status == CampaignStatus.ACTIVE && endsAt != null && !endsAt.isAfter(now)) {
            status = CampaignStatus.EXPIRED;
            return true;
        }
        return false;
    }

    private static void validateGoal(BigDecimal goalAmount) {
        if (goalAmount == null || goalAmount.signum() <= 0) {
            throw new BusinessRuleException("Goal amount must be positive");
        }
    }

    private static void validateDeadline(Instant endsAt) {
        if (endsAt != null && !endsAt.isAfter(Instant.now())) {
            throw new BusinessRuleException("Campaign deadline must be in the future");
        }
    }

    public void receiveDonation(BigDecimal amount) {
        if (status == CampaignStatus.EXPIRED || (endsAt != null && !endsAt.isAfter(Instant.now()))) {
            throw new BusinessRuleException("Campaign has expired");
        }
        if (status != CampaignStatus.ACTIVE) {
            throw new BusinessRuleException("Campaign is not active");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Donation amount must be positive");
        }
        raisedAmount = raisedAmount.add(amount);
        if (raisedAmount.compareTo(goalAmount) >= 0) {
            status = CampaignStatus.COMPLETED;
        }
    }

    public void cancel() {
        if (status != CampaignStatus.ACTIVE) {
            throw new BusinessRuleException("Only active campaigns can be cancelled");
        }
        status = CampaignStatus.CANCELLED;
    }

    public BigDecimal progressPercentage() {
        BigDecimal percentage = raisedAmount.multiply(BigDecimal.valueOf(100))
                .divide(goalAmount, 2, RoundingMode.HALF_UP);
        return percentage.min(BigDecimal.valueOf(100).setScale(2));
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

    public BigDecimal getGoalAmount() {
        return goalAmount;
    }

    public BigDecimal getRaisedAmount() {
        return raisedAmount;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public CampaignCategory getCategory() {
        return category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
