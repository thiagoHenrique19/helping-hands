package com.helpinghands.core.port.in.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PublicDonationResponse(Long id, String donorName, BigDecimal amount, String message, Instant createdAt) {
}
