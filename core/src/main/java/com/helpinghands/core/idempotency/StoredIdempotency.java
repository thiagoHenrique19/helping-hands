package com.helpinghands.core.idempotency;

public record StoredIdempotency(String requestHash, String responseBody) {
}
