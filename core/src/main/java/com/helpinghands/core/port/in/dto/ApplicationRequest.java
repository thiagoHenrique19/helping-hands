package com.helpinghands.core.port.in.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationRequest(
        @NotBlank @Size(max = 255) String volunteerName,
        @NotBlank @Email String volunteerEmail,
        @Size(max = 30) String volunteerPhone,
        @Size(max = 2000) String motivation) {
}
