package com.exchange.task.service;

import com.exchange.task.model.Order;
import com.exchange.task.model.OrderStatus;
import com.exchange.task.model.UserBalance;
import com.exchange.task.repository.OrderRepository;
import com.exchange.task.repository.UserBalanceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserBalanceRepository userBalanceRepository;

    public OrderService(OrderRepository orderRepository, UserBalanceRepository userBalanceRepository) {
        this.orderRepository = orderRepository;
        this.userBalanceRepository = userBalanceRepository;
    }

    public Order createLimitBuyOrder(String clientOrderId, String userId, String symbol, BigDecimal price, BigDecimal quantity) {
        validateCreateRequest(clientOrderId, userId, symbol, price, quantity);

        UserBalance balance = userBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("user balance not found"));

        BigDecimal amount = price.multiply(quantity);

        if (balance.getAvailableUsdt().compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient balance");
        }

        Order order = new Order(clientOrderId, userId, symbol, price, quantity);
        return orderRepository.save(order);
    }

    public Order cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("order already cancelled");
        }

        order.cancel();
        return orderRepository.save(order);
    }

    private void validateCreateRequest(String clientOrderId, String userId, String symbol, BigDecimal price, BigDecimal quantity) {
        if (clientOrderId == null || clientOrderId.isBlank()) {
            throw new IllegalArgumentException("clientOrderId is blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is blank");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is blank");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
