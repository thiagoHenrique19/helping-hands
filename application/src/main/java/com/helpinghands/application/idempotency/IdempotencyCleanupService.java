package com.helpinghands.application.idempotency;

import com.helpinghands.core.port.in.PurgeIdempotencyKeysUseCase;
import com.helpinghands.core.port.out.IdempotencyStore;
import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyCleanupService implements PurgeIdempotencyKeysUseCase {

    private final IdempotencyStore store;
    private final Clock clock;

    public IdempotencyCleanupService(IdempotencyStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int purgeOlderThan(Duration retention) {
        return store.deleteCreatedBefore(clock.instant().minus(retention));
    }
}
