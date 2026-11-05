package com.helpinghands.core.exception;

public class IdempotencyKeyReusedException extends BusinessRuleException {

    public IdempotencyKeyReusedException() {
        super("This Idempotency-Key was already used with a different request");
    }
}
