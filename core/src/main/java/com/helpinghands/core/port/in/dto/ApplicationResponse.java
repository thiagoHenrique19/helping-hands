package com.helpinghands.core.port.in.dto;

import com.helpinghands.core.domain.ApplicationStatus;
import java.time.Instant;

public record ApplicationResponse(
        Long id,
        Long opportunityId,
        String volunteerName,
        String volunteerEmail,
        String volunteerPhone,
        String motivation,
        ApplicationStatus status,
        Instant createdAt) {
}
