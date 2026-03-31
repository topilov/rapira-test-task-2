package com.exchange.task.exception;

public class InvalidOrderRequestException extends IllegalArgumentException {

    public InvalidOrderRequestException(String message) {
        super(message);
    }
}
