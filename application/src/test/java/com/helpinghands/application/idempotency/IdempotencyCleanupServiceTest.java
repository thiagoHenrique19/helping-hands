package com.helpinghands.application.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpinghands.core.port.out.IdempotencyStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyCleanupServiceTest {

    @Mock private IdempotencyStore store;

    private final Instant now = Instant.parse("2026-03-10T12:00:00Z");

    @Test
    void shouldDeleteEverythingOlderThanTheRetentionAndReturnTheCount() {
        when(store.deleteCreatedBefore(Instant.parse("2026-03-09T12:00:00Z"))).thenReturn(4);
        var service = new IdempotencyCleanupService(store, Clock.fixed(now, ZoneOffset.UTC));

        assertThat(service.purgeOlderThan(Duration.ofHours(24))).isEqualTo(4);
    }

    @Test
    void shouldMeasureTheCutoffFromTheClock() {
        when(store.deleteCreatedBefore(now)).thenReturn(0);
        var service = new IdempotencyCleanupService(store, Clock.fixed(now, ZoneOffset.UTC));

        service.purgeOlderThan(Duration.ZERO);

        verify(store).deleteCreatedBefore(now);
    }
}
