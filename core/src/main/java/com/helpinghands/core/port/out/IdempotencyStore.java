package com.helpinghands.core.port.out;

import com.helpinghands.core.idempotency.StoredIdempotency;
import java.time.Instant;
import java.util.Optional;

public interface IdempotencyStore {

    /**
     * Claims the key. Returns false when somebody already holds it. If another transaction is claiming the
     * same key right now this call waits for it, so a false result always means the other claim is committed.
     */
    boolean reserve(String scope, String key, String requestHash);

    Optional<StoredIdempotency> find(String scope, String key);

    void complete(String scope, String key, String responseBody);

    int deleteCreatedBefore(Instant cutoff);
}
