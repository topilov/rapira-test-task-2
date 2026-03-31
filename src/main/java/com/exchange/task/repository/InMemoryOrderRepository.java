package com.exchange.task.repository;

import com.exchange.task.model.Order;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class InMemoryOrderRepository implements OrderRepository {
    private final Map<String, Order> orders = new HashMap<>();

    @Override
    public Order save(Order order) {
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public Optional<Order> findByUserIdAndClientOrderId(String userId, String clientOrderId) {
        return orders.values().stream()
                .filter(o -> o.getUserId().equals(userId) && o.getClientOrderId().equals(clientOrderId))
                .findFirst();
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(orders.values());
    }
}
