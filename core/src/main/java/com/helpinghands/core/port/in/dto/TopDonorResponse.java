package com.helpinghands.core.port.in.dto;

import java.math.BigDecimal;

public record TopDonorResponse(String donorName, BigDecimal totalAmount, long donationsCount) {
}
