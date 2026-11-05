package com.helpinghands.core.port.in.dto;

import com.helpinghands.core.domain.Role;

public record UserResponse(Long id, String email, Role role, Long organizationId, boolean active) {
}
