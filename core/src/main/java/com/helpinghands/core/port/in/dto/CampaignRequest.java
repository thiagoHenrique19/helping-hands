package com.helpinghands.core.port.in.dto;

import com.helpinghands.core.domain.CampaignCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record CampaignRequest(
        @NotBlank String title,
        String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal goalAmount,
        Instant endsAt,
        CampaignCategory category) {
}
