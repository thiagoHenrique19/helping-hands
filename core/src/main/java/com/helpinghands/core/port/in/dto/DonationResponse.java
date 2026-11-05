package com.helpinghands.core.port.in.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DonationResponse(Long id, Long campaignId, String donorName, BigDecimal amount, Instant createdAt) {
}
