package com.helpinghands.core.port.in.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DonationRequest(
        @NotBlank String donorName,
        @NotBlank @Email String donorEmail,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
