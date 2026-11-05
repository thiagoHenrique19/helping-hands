package com.helpinghands.core.idempotency;

import com.helpinghands.core.exception.InvalidRequestException;

public record IdempotencyKey(String value) {

    public static final int MAX_LENGTH = 255;

    public static IdempotencyKey of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidRequestException("Idempotency-Key must not be blank");
        }
        String trimmed = raw.strip();
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidRequestException("Idempotency-Key must have at most " + MAX_LENGTH + " characters");
        }
        if (!trimmed.chars().allMatch(c -> c > 0x20 && c < 0x7f)) {
            throw new InvalidRequestException("Idempotency-Key may only contain printable ASCII characters");
        }
        return new IdempotencyKey(trimmed);
    }

    /** Keys only have to be unique per resource, so the same key may be reused on another campaign. */
    public String scopedTo(String operation, Long resourceId) {
        return operation + ":" + resourceId;
    }
}
