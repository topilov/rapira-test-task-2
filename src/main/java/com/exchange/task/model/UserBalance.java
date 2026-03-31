package com.exchange.task.model;

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

    public void setAvailableUsdt(BigDecimal availableUsdt) {
        this.availableUsdt = availableUsdt;
    }

    public BigDecimal getReservedUsdt() {
        return reservedUsdt;
    }

    public void setReservedUsdt(BigDecimal reservedUsdt) {
        this.reservedUsdt = reservedUsdt;
    }
}
