package com.exchange.task.service;

import com.exchange.task.model.Order;

public record CreateOrderResult(Order order, boolean created) {
}
