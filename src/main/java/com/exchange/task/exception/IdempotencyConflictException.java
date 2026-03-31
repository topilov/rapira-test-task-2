package com.exchange.task.exception;

public class IdempotencyConflictException extends IllegalStateException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
