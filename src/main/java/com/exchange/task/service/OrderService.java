package com.exchange.task.service;

import com.exchange.task.exception.IdempotencyConflictException;
import com.exchange.task.exception.InvalidOrderRequestException;
import com.exchange.task.exception.OrderNotFoundException;
import com.exchange.task.model.Order;
import com.exchange.task.model.UserBalance;
import com.exchange.task.repository.OrderRepository;
import com.exchange.task.repository.UserBalanceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserBalanceRepository userBalanceRepository;

    public OrderService(OrderRepository orderRepository,
                        UserBalanceRepository userBalanceRepository) {
        this.orderRepository = orderRepository;
        this.userBalanceRepository = userBalanceRepository;
    }

    public CreateOrderResult createLimitBuyOrderResult(String clientOrderId, String userId, String symbol, BigDecimal price, BigDecimal quantity) {
        validateCreateRequest(clientOrderId, userId, symbol, price, quantity);

        Order existingOrder = orderRepository.findByUserIdAndClientOrderId(userId, clientOrderId).orElse(null);
        if (existingOrder != null) {
            if (!existingOrder.matchesPayload(symbol, price, quantity)) {
                throw new IdempotencyConflictException("clientOrderId already exists with different payload");
            }
            return new CreateOrderResult(existingOrder, false);
        }

        UserBalance currentBalance = userBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new InvalidOrderRequestException("user balance not found"));
        UserBalance updatedBalance = currentBalance.copy();

        updatedBalance.reserve(price.multiply(quantity));

        Order order = new Order(clientOrderId, userId, symbol, price, quantity);
        Order savedOrder = orderRepository.save(order);

        try {
            userBalanceRepository.save(updatedBalance);
            return new CreateOrderResult(savedOrder, true);
        } catch (RuntimeException exception) {
            orderRepository.deleteById(savedOrder.getId());
            throw exception;
        }
    }

    public Order cancelOrder(String orderId) {
        Order currentOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("order not found"));
        Order cancelledOrder = currentOrder.copy();
        cancelledOrder.cancel();

        UserBalance currentBalance = userBalanceRepository.findByUserId(currentOrder.getUserId())
                .orElseThrow(() -> new InvalidOrderRequestException("user balance not found"));
        UserBalance updatedBalance = currentBalance.copy();
        updatedBalance.release(currentOrder.getAmount());

        Order savedOrder = orderRepository.save(cancelledOrder);

        try {
            userBalanceRepository.save(updatedBalance);
            return savedOrder;
        } catch (RuntimeException exception) {
            try {
                orderRepository.save(currentOrder);
            } catch (RuntimeException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw exception;
        }
    }

    private void validateCreateRequest(String clientOrderId, String userId, String symbol, BigDecimal price, BigDecimal quantity) {
        if (clientOrderId == null || clientOrderId.isBlank()) {
            throw new InvalidOrderRequestException("clientOrderId is blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new InvalidOrderRequestException("userId is blank");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new InvalidOrderRequestException("symbol is blank");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderRequestException("price must be positive");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderRequestException("quantity must be positive");
        }
    }
}
