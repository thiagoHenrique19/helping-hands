package com.helpinghands.core.port.in;

import java.time.Duration;

public interface PurgeIdempotencyKeysUseCase {

    /** Forgets keys older than the retention, after which a retry is treated as a new request. */
    int purgeOlderThan(Duration retention);
}
