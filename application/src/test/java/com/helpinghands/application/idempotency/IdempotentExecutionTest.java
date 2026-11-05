package com.helpinghands.application.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.helpinghands.core.exception.BusinessRuleException;
import com.helpinghands.core.exception.ConflictException;
import com.helpinghands.core.exception.IdempotencyKeyReusedException;
import com.helpinghands.core.exception.InvalidRequestException;
import com.helpinghands.core.idempotency.StoredIdempotency;
import com.helpinghands.core.port.out.IdempotencyPayloads;
import com.helpinghands.core.port.out.IdempotencyStore;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotentExecutionTest {

    record Response(int number) {
    }

    @Mock private IdempotencyStore store;
    @Mock private IdempotencyPayloads payloads;

    private IdempotentExecution execution;
    private final AtomicInteger runs = new AtomicInteger();

    @BeforeEach
    void setUp() {
        execution = new IdempotentExecution(store, payloads);
    }

    private Response action() {
        return new Response(runs.incrementAndGet());
    }

    @Test
    void shouldJustRunTheActionWhenNoKeyIsSent() {
        Response response = execution.run("donation:1", null, "request", Response.class, this::action);

        assertThat(response.number()).isEqualTo(1);
        verifyNoInteractions(store, payloads);
    }

    @Test
    void shouldRunAndSnapshotTheResponseForANewKey() {
        when(payloads.fingerprint("request")).thenReturn("hash");
        when(store.reserve("donation:1", "key-1", "hash")).thenReturn(true);
        when(payloads.serialize(new Response(1))).thenReturn("{\"number\":1}");

        Response response = execution.run("donation:1", "key-1", "request", Response.class, this::action);

        assertThat(response).isEqualTo(new Response(1));
        verify(store).complete("donation:1", "key-1", "{\"number\":1}");
    }

    @Test
    void shouldReplayTheStoredResponseWithoutRunningTheActionAgain() {
        when(payloads.fingerprint("request")).thenReturn("hash");
        when(store.reserve("donation:1", "key-1", "hash")).thenReturn(false);
        when(store.find("donation:1", "key-1")).thenReturn(Optional.of(new StoredIdempotency("hash", "{\"number\":1}")));
        when(payloads.deserialize("{\"number\":1}", Response.class)).thenReturn(new Response(1));

        Response response = execution.run("donation:1", "key-1", "request", Response.class, this::action);

        assertThat(response).isEqualTo(new Response(1));
        assertThat(runs).hasValue(0);
        verify(store, never()).complete(anyString(), anyString(), anyString());
    }

    @Test
    void shouldRejectTheSameKeyWithADifferentPayload() {
        when(payloads.fingerprint("another request")).thenReturn("other-hash");
        when(store.reserve("donation:1", "key-1", "other-hash")).thenReturn(false);
        when(store.find("donation:1", "key-1")).thenReturn(Optional.of(new StoredIdempotency("hash", "{}")));

        assertThatThrownBy(() -> execution.run("donation:1", "key-1", "another request", Response.class, this::action))
                .isInstanceOf(IdempotencyKeyReusedException.class);
        assertThat(runs).hasValue(0);
    }

    @Test
    void shouldReportConflictWhenTheOriginalHasNoResponseYet() {
        when(payloads.fingerprint("request")).thenReturn("hash");
        when(store.reserve("donation:1", "key-1", "hash")).thenReturn(false);
        when(store.find("donation:1", "key-1")).thenReturn(Optional.of(new StoredIdempotency("hash", null)));

        assertThatThrownBy(() -> execution.run("donation:1", "key-1", "request", Response.class, this::action))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldRejectInvalidKeysBeforeTouchingAnything() {
        assertThatThrownBy(() -> execution.run("donation:1", "has space", "request", Response.class, this::action))
                .isInstanceOf(InvalidRequestException.class);
        assertThat(runs).hasValue(0);
        verifyNoInteractions(store);
    }

    @Test
    void shouldNotStoreAResponseWhenTheActionFails() {
        when(payloads.fingerprint("request")).thenReturn("hash");
        when(store.reserve("donation:1", "key-1", "hash")).thenReturn(true);

        assertThatThrownBy(() -> execution.run("donation:1", "key-1", "request", Response.class, () -> {
            throw new BusinessRuleException("Campaign is not active");
        })).isInstanceOf(BusinessRuleException.class);

        verify(store, never()).complete(anyString(), anyString(), any());
    }
}
