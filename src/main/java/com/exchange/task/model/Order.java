package com.exchange.task.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Order {
    private final String id;
    private final String clientOrderId;
    private final String userId;
    private final String symbol;
    private final BigDecimal price;
    private final BigDecimal quantity;
    private OrderStatus status;

    public Order(String clientOrderId, String userId, String symbol, BigDecimal price, BigDecimal quantity) {
        this.id = UUID.randomUUID().toString();
        this.clientOrderId = clientOrderId;
        this.userId = userId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.status = OrderStatus.NEW;
    }

    public String getId() {
        return id;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }
}
