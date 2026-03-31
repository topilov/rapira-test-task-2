package com.exchange.task.repository;

import com.exchange.task.model.Order;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class InMemoryOrderRepository implements OrderRepository {
    private final Map<String, Order> orders = new HashMap<>();

    @Override
    public Order save(Order order) {
        Order storedOrder = order.copy();
        orders.put(storedOrder.getId(), storedOrder);
        return storedOrder.copy();
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id))
                .map(Order::copy);
    }

    @Override
    public Optional<Order> findByUserIdAndClientOrderId(String userId, String clientOrderId) {
        return orders.values().stream()
                .filter(o -> o.getUserId().equals(userId) && o.getClientOrderId().equals(clientOrderId))
                .map(Order::copy)
                .findFirst();
    }

    @Override
    public void deleteById(String id) {
        orders.remove(id);
    }

    @Override
    public List<Order> findAll() {
        return orders.values().stream()
                .map(Order::copy)
                .toList();
    }
}
