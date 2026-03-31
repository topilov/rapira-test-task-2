package com.exchange.task.model;

import com.exchange.task.exception.OrderStateException;

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

    private Order(Order source) {
        this.id = source.id;
        this.clientOrderId = source.clientOrderId;
        this.userId = source.userId;
        this.symbol = source.symbol;
        this.price = source.price;
        this.quantity = source.quantity;
        this.status = source.status;
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

    public BigDecimal getAmount() {
        return price.multiply(quantity);
    }

    public boolean matchesPayload(String symbol, BigDecimal price, BigDecimal quantity) {
        return this.symbol.equals(symbol)
                && this.price.compareTo(price) == 0
                && this.quantity.compareTo(quantity) == 0;
    }

    public void cancel() {
        if (status != OrderStatus.NEW) {
            throw new OrderStateException("only NEW order can be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public Order copy() {
        return new Order(this);
    }
}
