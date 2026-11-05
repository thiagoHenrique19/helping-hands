package com.helpinghands.core.port.in.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrganizationRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String description,
        @Size(max = 30) String phone,
        @Size(max = 255) @Pattern(regexp = "^(https?://.+)?$", message = "must be a valid http(s) URL") String website) {
}
