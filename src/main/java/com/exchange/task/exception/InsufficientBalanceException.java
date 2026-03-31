package com.exchange.task.exception;

public class InsufficientBalanceException extends IllegalStateException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
