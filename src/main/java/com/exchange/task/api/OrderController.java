package com.exchange.task.api;

import com.exchange.task.model.Order;
import com.exchange.task.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createLimitBuyOrder(
                request.getClientOrderId(),
                request.getUserId(),
                request.getSymbol(),
                request.getPrice(),
                request.getQuantity()
        );
    }

    @PostMapping("/{orderId}/cancel")
    public Order cancelOrder(@PathVariable String orderId) {
        return orderService.cancelOrder(orderId);
    }
}
