package com.exchange.task.api;

import com.exchange.task.model.Order;
import com.exchange.task.model.OrderStatus;

import java.math.BigDecimal;

public record OrderResponse(
        String id,
        String clientOrderId,
        String userId,
        String symbol,
        BigDecimal price,
        BigDecimal quantity,
        OrderStatus status
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getClientOrderId(),
                order.getUserId(),
                order.getSymbol(),
                order.getPrice(),
                order.getQuantity(),
                order.getStatus()
        );
    }
}
