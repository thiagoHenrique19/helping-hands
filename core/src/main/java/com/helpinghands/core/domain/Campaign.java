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
import java.math.BigDecimal;
import java.math.RoundingMode;

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

    protected Campaign() {
    }

    private Campaign(Organization organization, String title, String description, BigDecimal goalAmount) {
        this.organization = organization;
        this.title = title;
        this.description = description;
        this.goalAmount = goalAmount;
    }

    public static Campaign create(Organization organization, String title, String description, BigDecimal goalAmount) {
        if (goalAmount == null || goalAmount.signum() <= 0) {
            throw new BusinessRuleException("Goal amount must be positive");
        }
        return new Campaign(organization, title, description, goalAmount);
    }

    public void receiveDonation(BigDecimal amount) {
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
        if (status == CampaignStatus.COMPLETED) {
            throw new BusinessRuleException("Completed campaign cannot be cancelled");
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
}
