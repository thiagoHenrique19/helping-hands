package com.helpinghands.infrastructure.scheduling;

import com.helpinghands.core.port.in.PurgeIdempotencyKeysUseCase;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyKeyPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyPurgeScheduler.class);

    private final PurgeIdempotencyKeysUseCase purgeUseCase;
    private final Duration retention;

    public IdempotencyKeyPurgeScheduler(PurgeIdempotencyKeysUseCase purgeUseCase,
                                        @Value("${app.idempotency.retention:24h}") Duration retention) {
        this.purgeUseCase = purgeUseCase;
        this.retention = retention;
    }

    @Scheduled(cron = "${app.idempotency.purge-cron:0 15 * * * *}")
    public void purge() {
        int removed = purgeUseCase.purgeOlderThan(retention);
        if (removed > 0) {
            log.info("Removed {} expired idempotency key(s)", removed);
        }
    }
}
