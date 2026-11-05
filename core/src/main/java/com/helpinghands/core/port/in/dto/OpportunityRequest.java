package com.helpinghands.core.port.in.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record OpportunityRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description,
        @Size(max = 255) String location,
        boolean remote,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @Min(1) int slots) {
}
