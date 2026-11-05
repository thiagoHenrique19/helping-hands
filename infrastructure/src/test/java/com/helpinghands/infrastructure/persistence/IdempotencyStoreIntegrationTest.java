package com.helpinghands.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.helpinghands.AbstractPostgresIntegrationTest;
import com.helpinghands.ConcurrentRunner;
import com.helpinghands.core.port.out.IdempotencyStore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class IdempotencyStoreIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String HASH = "a".repeat(64);

    @Autowired private IdempotencyStore store;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String key = UUID.randomUUID().toString();

    @Test
    void shouldReserveOnlyOnce() {
        assertThat(store.reserve("donation:1", key, HASH)).isTrue();
        assertThat(store.reserve("donation:1", key, HASH)).isFalse();
    }

    @Test
    void shouldKeepKeysSeparateBetweenScopes() {
        assertThat(store.reserve("donation:1", key, HASH)).isTrue();
        assertThat(store.reserve("donation:2", key, HASH)).isTrue();
    }

    @Test
    void shouldStoreAndReturnTheResponseSnapshot() {
        store.reserve("donation:1", key, HASH);
        assertThat(store.find("donation:1", key)).get()
                .satisfies(found -> assertThat(found.responseBody()).isNull());

        store.complete("donation:1", key, "{\"id\":9}");

        var found = store.find("donation:1", key).orElseThrow();
        assertThat(found.requestHash()).isEqualTo(HASH);
        assertThat(found.responseBody()).isEqualTo("{\"id\":9}");
    }

    @Test
    void shouldFindNothingForUnknownKey() {
        assertThat(store.find("donation:1", "missing")).isEmpty();
    }

    @Test
    void shouldLetExactlyOneOfManyParallelTransactionsWin() throws Exception {
        var transactions = new TransactionTemplate(transactionManager);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            tasks.add(() -> transactions.execute(status -> store.reserve("donation:5", key, HASH)));
        }

        var outcomes = ConcurrentRunner.runAll(tasks);

        assertThat(outcomes).allMatch(ConcurrentRunner.Outcome::succeeded);
        assertThat(outcomes).filteredOn(o -> Boolean.TRUE.equals(o.value())).hasSize(1);
    }

    @Test
    void shouldDeleteOnlyKeysOlderThanTheCutoff() {
        store.reserve("purge", "old-" + key, HASH);
        store.reserve("purge", "new-" + key, HASH);
        jdbc.update("UPDATE idempotency_key SET created_at = now() - interval '3 days' WHERE key_value = ?",
                "old-" + key);

        int deleted = store.deleteCreatedBefore(Instant.now().minus(1, ChronoUnit.DAYS));

        assertThat(deleted).isGreaterThanOrEqualTo(1);
        assertThat(store.find("purge", "old-" + key)).isEmpty();
        assertThat(store.find("purge", "new-" + key)).isPresent();
    }
}
