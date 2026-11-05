package com.helpinghands.application.idempotency;

import com.helpinghands.core.exception.ConflictException;
import com.helpinghands.core.exception.IdempotencyKeyReusedException;
import com.helpinghands.core.idempotency.IdempotencyKey;
import com.helpinghands.core.port.out.IdempotencyPayloads;
import com.helpinghands.core.port.out.IdempotencyStore;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Meant to be called from inside the use case's transaction: the key claim, the action and the stored
 * response commit or roll back together. A failed request therefore leaves no key behind and may be retried.
 */
@Component
public class IdempotentExecution {

    private final IdempotencyStore store;
    private final IdempotencyPayloads payloads;

    public IdempotentExecution(IdempotencyStore store, IdempotencyPayloads payloads) {
        this.store = store;
        this.payloads = payloads;
    }

    public <T> T run(String scope, String rawKey, Object request, Class<T> responseType, Supplier<T> action) {
        if (rawKey == null) {
            return action.get();
        }
        String key = IdempotencyKey.of(rawKey).value();
        String hash = payloads.fingerprint(request);
        if (store.reserve(scope, key, hash)) {
            T response = action.get();
            store.complete(scope, key, payloads.serialize(response));
            return response;
        }
        var stored = store.find(scope, key).orElseThrow(() -> new ConflictException("Idempotency key is being released, retry"));
        if (!stored.requestHash().equals(hash)) {
            throw new IdempotencyKeyReusedException();
        }
        if (stored.responseBody() == null) {
            throw new ConflictException("A request with this Idempotency-Key is still being processed");
        }
        return payloads.deserialize(stored.responseBody(), responseType);
    }
}
