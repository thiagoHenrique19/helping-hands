package com.helpinghands.core.port.in.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyDonations(LocalDate month, long donationsCount, BigDecimal totalAmount) {
}
