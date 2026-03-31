package com.exchange.task.repository;

import com.exchange.task.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String id);
    Optional<Order> findByUserIdAndClientOrderId(String userId, String clientOrderId);
    List<Order> findAll();
}
