package com.helpinghands.core.port.in.dto;

import com.helpinghands.core.domain.OpportunityStatus;
import java.time.Instant;

public record OpportunityResponse(
        Long id,
        Long organizationId,
        String title,
        String description,
        String location,
        boolean remote,
        Instant startsAt,
        Instant endsAt,
        int slots,
        int filledSlots,
        int availableSlots,
        OpportunityStatus status) {
}
