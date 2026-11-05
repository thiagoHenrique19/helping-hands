package com.helpinghands.core.domain;

import jakarta.persistence.Column;
import com.helpinghands.core.exception.BusinessRuleException;
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
import java.time.Instant;

@Entity
public class Donation {

    public static final BigDecimal MINIMUM_AMOUNT = new BigDecimal("1.00");
    public static final String ANONYMOUS_DONOR_NAME = "Anonymous";

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

    private boolean anonymous;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String receiptCode;

    private Instant createdAt = Instant.now();

    protected Donation() {
    }

    private Donation(Campaign campaign, String donorName, String donorEmail, BigDecimal amount,
                     boolean anonymous, String message, PaymentMethod paymentMethod, String receiptCode) {
        this.campaign = campaign;
        this.donorName = donorName;
        this.donorEmail = donorEmail;
        this.amount = amount;
        this.anonymous = anonymous;
        this.message = message;
        this.paymentMethod = paymentMethod;
        this.receiptCode = receiptCode;
    }

    public static Donation make(Campaign campaign, String donorName, String donorEmail, BigDecimal amount,
                                boolean anonymous, String message, PaymentMethod paymentMethod,
                                String receiptCode) {
        if (amount == null || amount.compareTo(MINIMUM_AMOUNT) < 0) {
            throw new BusinessRuleException("Donation amount must be at least the minimum of " + MINIMUM_AMOUNT);
        }
        if (paymentMethod == null) {
            throw new BusinessRuleException("Payment method is required");
        }
        return new Donation(campaign, donorName, donorEmail, amount, anonymous, message, paymentMethod, receiptCode);
    }

    public String publicDonorName() {
        return anonymous ? ANONYMOUS_DONOR_NAME : donorName;
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

    public boolean isAnonymous() {
        return anonymous;
    }

    public String getMessage() {
        return message;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getReceiptCode() {
        return receiptCode;
    }
}
