package com.exchange.task.exception;

public class OrderNotFoundException extends IllegalArgumentException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}
