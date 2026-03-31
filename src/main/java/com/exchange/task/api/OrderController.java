package com.exchange.task.api;

import com.exchange.task.service.CreateOrderResult;
import com.exchange.task.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        CreateOrderResult result = orderService.createLimitBuyOrderResult(
                request.getClientOrderId(),
                request.getUserId(),
                request.getSymbol(),
                request.getPrice(),
                request.getQuantity()
        );

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(OrderResponse.from(result.order()));
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable String orderId) {
        return OrderResponse.from(orderService.cancelOrder(orderId));
    }
}
