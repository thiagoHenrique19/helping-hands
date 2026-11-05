package com.helpinghands.infrastructure.scheduling;

import static org.mockito.Mockito.verify;

import com.helpinghands.core.port.in.PurgeIdempotencyKeysUseCase;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyKeyPurgeSchedulerTest {

    @Mock private PurgeIdempotencyKeysUseCase purge;

    @Test
    void shouldPurgeWithTheConfiguredRetention() {
        new IdempotencyKeyPurgeScheduler(purge, Duration.ofHours(6)).purge();

        verify(purge).purgeOlderThan(Duration.ofHours(6));
    }
}
