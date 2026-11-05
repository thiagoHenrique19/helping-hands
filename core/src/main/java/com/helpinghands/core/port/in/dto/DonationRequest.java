package com.helpinghands.core.port.in.dto;

import com.helpinghands.core.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DonationRequest(
        @NotBlank String donorName,
        @NotBlank @Email String donorEmail,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        boolean anonymous,
        @Size(max = 500) String message,
        @NotNull PaymentMethod paymentMethod) {
}
