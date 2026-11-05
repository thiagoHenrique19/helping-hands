package com.helpinghands.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    private String donorName;
    private String donorEmail;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    private Instant createdAt = Instant.now();

    protected Donation() {
    }

    private Donation(Campaign campaign, String donorName, String donorEmail, BigDecimal amount) {
        this.campaign = campaign;
        this.donorName = donorName;
        this.donorEmail = donorEmail;
        this.amount = amount;
    }

    public static Donation make(Campaign campaign, String donorName, String donorEmail, BigDecimal amount) {
        return new Donation(campaign, donorName, donorEmail, amount);
    }

    public Long getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public String getDonorName() {
        return donorName;
    }

    public String getDonorEmail() {
        return donorEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
