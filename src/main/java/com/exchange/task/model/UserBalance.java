package com.exchange.task.model;

import com.exchange.task.exception.BalanceStateException;
import com.exchange.task.exception.InsufficientBalanceException;

import java.math.BigDecimal;

public class UserBalance {
    private final String userId;
    private BigDecimal availableUsdt;
    private BigDecimal reservedUsdt;

    public UserBalance(String userId, BigDecimal availableUsdt, BigDecimal reservedUsdt) {
        this.userId = userId;
        this.availableUsdt = availableUsdt;
        this.reservedUsdt = reservedUsdt;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getAvailableUsdt() {
        return availableUsdt;
    }

    public BigDecimal getReservedUsdt() {
        return reservedUsdt;
    }

    public void reserve(BigDecimal amount) {
        requirePositive(amount);
        if (availableUsdt.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("insufficient balance");
        }

        availableUsdt = availableUsdt.subtract(amount);
        reservedUsdt = reservedUsdt.add(amount);
    }

    public void release(BigDecimal amount) {
        requirePositive(amount);
        if (reservedUsdt.compareTo(amount) < 0) {
            throw new BalanceStateException("reserved balance is insufficient");
        }
        reservedUsdt = reservedUsdt.subtract(amount);
        availableUsdt = availableUsdt.add(amount);
    }

    public UserBalance copy() {
        return new UserBalance(userId, availableUsdt, reservedUsdt);
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
