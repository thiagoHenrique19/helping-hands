package com.helpinghands.core.port.in.dto;

public record OrganizationResponse(
        Long id,
        String name,
        String email,
        String description,
        String phone,
        String website,
        boolean verified) {
}
