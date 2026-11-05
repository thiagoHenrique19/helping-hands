package com.helpinghands.core.port.in.dto.analytics;

import com.helpinghands.core.domain.PaymentMethod;
import java.math.BigDecimal;

public record PaymentMethodBreakdown(PaymentMethod paymentMethod, long donationsCount, BigDecimal totalAmount) {
}
