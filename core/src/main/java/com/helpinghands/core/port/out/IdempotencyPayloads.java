package com.helpinghands.core.port.out;

public interface IdempotencyPayloads {

    /** Stable SHA-256 hex digest of a request: same content gives the same value whatever the field order. */
    String fingerprint(Object request);

    String serialize(Object response);

    <T> T deserialize(String snapshot, Class<T> type);
}
