package com.exchange.task.api;

import com.exchange.task.exception.BalanceStateException;
import com.exchange.task.exception.IdempotencyConflictException;
import com.exchange.task.exception.InsufficientBalanceException;
import com.exchange.task.exception.InvalidOrderRequestException;
import com.exchange.task.exception.OrderNotFoundException;
import com.exchange.task.exception.OrderStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidOrderRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrderRequest(InvalidOrderRequestException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientBalance(InsufficientBalanceException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotencyConflict(IdempotencyConflictException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(OrderStateException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderState(OrderStateException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(OrderNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(BalanceStateException.class)
    public ResponseEntity<ApiErrorResponse> handleBalanceState(BalanceStateException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, RuntimeException exception) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(exception.getMessage()));
    }
}
