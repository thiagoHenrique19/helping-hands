package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.idempotency.StoredIdempotency;
import com.helpinghands.core.port.out.IdempotencyStore;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcIdempotencyStore implements IdempotencyStore {

    private final JdbcTemplate jdbc;

    JdbcIdempotencyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean reserve(String scope, String key, String requestHash) {
        return jdbc.update("""
                INSERT INTO idempotency_key (scope, key_value, request_hash)
                VALUES (?, ?, ?)
                ON CONFLICT (scope, key_value) DO NOTHING
                """, scope, key, requestHash) == 1;
    }

    @Override
    public Optional<StoredIdempotency> find(String scope, String key) {
        return jdbc.query("SELECT request_hash, response_body FROM idempotency_key WHERE scope = ? AND key_value = ?",
                (rs, row) -> new StoredIdempotency(rs.getString("request_hash"), rs.getString("response_body")),
                scope, key).stream().findFirst();
    }

    @Override
    public void complete(String scope, String key, String responseBody) {
        jdbc.update("UPDATE idempotency_key SET response_body = ? WHERE scope = ? AND key_value = ?",
                responseBody, scope, key);
    }

    @Override
    public int deleteCreatedBefore(Instant cutoff) {
        return jdbc.update("DELETE FROM idempotency_key WHERE created_at < ?", Timestamp.from(cutoff));
    }
}
