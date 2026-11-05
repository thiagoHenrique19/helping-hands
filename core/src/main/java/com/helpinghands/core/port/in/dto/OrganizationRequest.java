package com.helpinghands.core.port.in.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OrganizationRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String description) {
}
