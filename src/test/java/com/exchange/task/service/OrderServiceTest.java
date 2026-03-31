package com.exchange.task.service;

import com.exchange.task.model.Order;
import com.exchange.task.model.OrderStatus;
import com.exchange.task.model.UserBalance;
import com.exchange.task.repository.InMemoryOrderRepository;
import com.exchange.task.repository.OrderRepository;
import com.exchange.task.repository.UserBalanceRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    @Test
    void shouldCreateOrderWhenBalanceIsEnough() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        TestUserBalanceRepository balanceRepository = new TestUserBalanceRepository();
        balanceRepository.save(new UserBalance("user-1", new BigDecimal("1000.00"), BigDecimal.ZERO));

        OrderService service = new OrderService(orderRepository, balanceRepository);

        Order order = service.createLimitBuyOrder(
                "client-1",
                "user-1",
                "BTCUSDT",
                new BigDecimal("100.00"),
                new BigDecimal("2.00")
        );

        assertNotNull(order);
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertEquals(1, orderRepository.findAll().size());
    }

    @Test
    void shouldFailWhenBalanceIsNotEnough() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        TestUserBalanceRepository balanceRepository = new TestUserBalanceRepository();
        balanceRepository.save(new UserBalance("user-1", new BigDecimal("50.00"), BigDecimal.ZERO));

        OrderService service = new OrderService(orderRepository, balanceRepository);

        assertThrows(IllegalStateException.class, () -> service.createLimitBuyOrder(
                "client-1",
                "user-1",
                "BTCUSDT",
                new BigDecimal("100.00"),
                new BigDecimal("1.00")
        ));

        assertEquals(0, orderRepository.findAll().size());
    }

    @Test
    void shouldCancelOrder() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        TestUserBalanceRepository balanceRepository = new TestUserBalanceRepository();
        balanceRepository.save(new UserBalance("user-1", new BigDecimal("1000.00"), BigDecimal.ZERO));

        OrderService service = new OrderService(orderRepository, balanceRepository);

        Order created = service.createLimitBuyOrder(
                "client-1",
                "user-1",
                "BTCUSDT",
                new BigDecimal("100.00"),
                new BigDecimal("1.00")
        );

        Order cancelled = service.cancelOrder(created.getId());

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
    }

    static class TestUserBalanceRepository implements UserBalanceRepository {
        private final Map<String, UserBalance> storage = new HashMap<>();

        @Override
        public Optional<UserBalance> findByUserId(String userId) {
            return Optional.ofNullable(storage.get(userId));
        }

        @Override
        public UserBalance save(UserBalance balance) {
            storage.put(balance.getUserId(), balance);
            return balance;
        }
    }
}
