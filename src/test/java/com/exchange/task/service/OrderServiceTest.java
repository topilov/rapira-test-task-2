package com.exchange.task.service;

import com.exchange.task.exception.IdempotencyConflictException;
import com.exchange.task.exception.InsufficientBalanceException;
import com.exchange.task.exception.InvalidOrderRequestException;
import com.exchange.task.exception.OrderNotFoundException;
import com.exchange.task.exception.OrderStateException;
import com.exchange.task.model.Order;
import com.exchange.task.model.OrderStatus;
import com.exchange.task.model.UserBalance;
import com.exchange.task.repository.InMemoryOrderRepository;
import com.exchange.task.repository.InMemoryUserBalanceRepository;
import com.exchange.task.repository.OrderRepository;
import com.exchange.task.repository.UserBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayNameGeneration(ReplaceUnderscores.class)
class OrderServiceTest {

    private static final String USER_ID = "user-1";
    private static final String CLIENT_ORDER_ID = "client-1";
    private static final String SYMBOL = "BTCUSDT";

    private ServiceFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ServiceFixture();
    }

    @Nested
    class CreateLimitBuyOrder {

        @Test
        void creates_a_new_order_and_reserves_balance() {
            fixture.givenBalance(USER_ID, "1000.00", "0.00");

            Order order = fixture.createLimitBuyOrder(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "2.00");

            assertAll(
                    () -> assertNotNull(order),
                    () -> assertEquals(OrderStatus.NEW, order.getStatus()),
                    () -> assertEquals(1, fixture.savedOrders()),
                    () -> fixture.assertBalance(USER_ID, "800.00", "200.00")
            );
        }

        @Test
        void rejects_order_when_balance_is_insufficient() {
            fixture.givenBalance(USER_ID, "50.00", "0.00");

            InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class,
                    () -> fixture.createLimitBuyOrder(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00"));

            assertAll(
                    () -> assertEquals("insufficient balance", exception.getMessage()),
                    () -> assertEquals(0, fixture.savedOrders())
            );
        }

        @Test
        void returns_existing_order_for_same_user_and_client_order_id() {
            fixture.givenBalance(USER_ID, "1000.00", "0.00");

            Order created = fixture.createLimitBuyOrder(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00");
            Order duplicated = fixture.createLimitBuyOrder(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00");

            assertAll(
                    () -> assertEquals(created.getId(), duplicated.getId()),
                    () -> assertNotSame(created, duplicated),
                    () -> assertEquals(1, fixture.savedOrders()),
                    () -> fixture.assertBalance(USER_ID, "900.00", "100.00")
            );
        }

        @Test
        void rejects_reused_client_order_id_when_payload_differs() {
            fixture.givenBalance(USER_ID, "1000.00", "0.00");
            fixture.createLimitBuyOrder(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00");

            IdempotencyConflictException exception = assertThrows(IdempotencyConflictException.class,
                    () -> fixture.createLimitBuyOrder(CLIENT_ORDER_ID, USER_ID, SYMBOL, "101.00", "1.00"));

            assertEquals("clientOrderId already exists with different payload", exception.getMessage());
        }

        @Test
        void rejects_blank_client_order_id() {
            InvalidOrderRequestException exception = assertThrows(InvalidOrderRequestException.class,
                    () -> fixture.createLimitBuyOrder(" ", USER_ID, SYMBOL, "100.00", "1.00"));

            assertEquals("clientOrderId is blank", exception.getMessage());
        }

        @Test
        void does_not_change_balance_when_order_persistence_fails() {
            FailingOrderRepository orderRepository = new FailingOrderRepository();
            InMemoryUserBalanceRepository balanceRepository = new InMemoryUserBalanceRepository();
            balanceRepository.save(new UserBalance(USER_ID, new BigDecimal("1000.00"), BigDecimal.ZERO));
            OrderService orderService = new OrderService(orderRepository, balanceRepository);

            assertThrows(RuntimeException.class,
                    () -> orderService.createLimitBuyOrderResult(
                            CLIENT_ORDER_ID,
                            USER_ID,
                            SYMBOL,
                            new BigDecimal("100.00"),
                            new BigDecimal("1.00")
                    ));

            UserBalance balance = balanceRepository.findByUserId(USER_ID).orElseThrow();
            assertAll(
                    () -> assertEquals(0, balance.getAvailableUsdt().compareTo(new BigDecimal("1000.00"))),
                    () -> assertEquals(0, balance.getReservedUsdt().compareTo(BigDecimal.ZERO))
            );
        }

        @Test
        void removes_created_order_when_balance_persistence_fails() {
            InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
            FailingUserBalanceRepository balanceRepository = new FailingUserBalanceRepository(USER_ID, "1000.00", "0.00");
            OrderService orderService = new OrderService(orderRepository, balanceRepository);

            assertThrows(RuntimeException.class,
                    () -> orderService.createLimitBuyOrderResult(
                            CLIENT_ORDER_ID,
                            USER_ID,
                            SYMBOL,
                            new BigDecimal("100.00"),
                            new BigDecimal("1.00")
                    ));

            assertEquals(0, orderRepository.findAll().size());
        }
    }

    @Nested
    class CancelOrder {

        @Test
        void cancels_new_order_and_releases_reserved_balance() {
            fixture.givenBalance(USER_ID, "1000.00", "0.00");
            Order created = fixture.createLimitBuyOrder(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00");

            Order cancelled = fixture.cancelOrder(created.getId());

            assertAll(
                    () -> assertEquals(OrderStatus.CANCELLED, cancelled.getStatus()),
                    () -> fixture.assertBalance(USER_ID, "1000.00", "0.00")
            );
        }

        @Test
        void rejects_repeated_cancellation() {
            fixture.givenBalance(USER_ID, "1000.00", "0.00");
            Order created = fixture.createLimitBuyOrder(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00");
            fixture.cancelOrder(created.getId());

            OrderStateException exception = assertThrows(OrderStateException.class,
                    () -> fixture.cancelOrder(created.getId()));

            assertEquals("only NEW order can be cancelled", exception.getMessage());
        }

        @Test
        void rejects_cancellation_for_unknown_order() {
            OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                    () -> fixture.cancelOrder("missing-order"));

            assertEquals("order not found", exception.getMessage());
        }

        @Test
        void does_not_change_balance_when_cancel_order_persistence_fails() {
            FailingCancelledOrderRepository orderRepository = new FailingCancelledOrderRepository();
            InMemoryUserBalanceRepository balanceRepository = new InMemoryUserBalanceRepository();
            OrderService orderService = new OrderService(orderRepository, balanceRepository);
            balanceRepository.save(new UserBalance(USER_ID, new BigDecimal("1000.00"), BigDecimal.ZERO));
            Order created = orderService.createLimitBuyOrderResult(
                    CLIENT_ORDER_ID,
                    USER_ID,
                    SYMBOL,
                    new BigDecimal("100.00"),
                    new BigDecimal("1.00")
            ).order();

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> orderService.cancelOrder(created.getId()));

            UserBalance balance = balanceRepository.findByUserId(USER_ID).orElseThrow();
            Order storedOrder = orderRepository.findById(created.getId()).orElseThrow();
            assertAll(
                    () -> assertEquals("order save failed", exception.getMessage()),
                    () -> assertEquals(OrderStatus.NEW, storedOrder.getStatus()),
                    () -> assertEquals(0, balance.getAvailableUsdt().compareTo(new BigDecimal("900.00"))),
                    () -> assertEquals(0, balance.getReservedUsdt().compareTo(new BigDecimal("100.00")))
            );
        }

        @Test
        void restores_order_when_balance_persistence_fails_during_cancel() {
            InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
            FailNextBalanceSaveRepository balanceRepository = new FailNextBalanceSaveRepository();
            OrderService orderService = new OrderService(orderRepository, balanceRepository);
            balanceRepository.save(new UserBalance(USER_ID, new BigDecimal("1000.00"), BigDecimal.ZERO));
            Order created = orderService.createLimitBuyOrderResult(
                    CLIENT_ORDER_ID,
                    USER_ID,
                    SYMBOL,
                    new BigDecimal("100.00"),
                    new BigDecimal("1.00")
            ).order();
            balanceRepository.failNextSave();

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> orderService.cancelOrder(created.getId()));

            UserBalance balance = balanceRepository.findByUserId(USER_ID).orElseThrow();
            Order storedOrder = orderRepository.findById(created.getId()).orElseThrow();
            assertAll(
                    () -> assertEquals("balance save failed", exception.getMessage()),
                    () -> assertEquals(OrderStatus.NEW, storedOrder.getStatus()),
                    () -> assertEquals(0, balance.getAvailableUsdt().compareTo(new BigDecimal("900.00"))),
                    () -> assertEquals(0, balance.getReservedUsdt().compareTo(new BigDecimal("100.00")))
            );
        }
    }

    private static final class ServiceFixture {

        private final InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        private final InMemoryUserBalanceRepository balanceRepository = new InMemoryUserBalanceRepository();
        private final OrderService orderService = new OrderService(orderRepository, balanceRepository);

        void givenBalance(String userId, String availableUsdt, String reservedUsdt) {
            balanceRepository.save(new UserBalance(userId, amount(availableUsdt), amount(reservedUsdt)));
        }

        Order createLimitBuyOrder(String clientOrderId, String userId, String symbol, String price, String quantity) {
            return orderService.createLimitBuyOrderResult(
                    clientOrderId,
                    userId,
                    symbol,
                    amount(price),
                    amount(quantity)
            ).order();
        }

        Order cancelOrder(String orderId) {
            return orderService.cancelOrder(orderId);
        }

        int savedOrders() {
            return orderRepository.findAll().size();
        }

        void assertBalance(String userId, String expectedAvailableUsdt, String expectedReservedUsdt) {
            UserBalance balance = balanceRepository.findByUserId(userId).orElseThrow();
            assertAll(
                    () -> assertAmountEquals(expectedAvailableUsdt, balance.getAvailableUsdt()),
                    () -> assertAmountEquals(expectedReservedUsdt, balance.getReservedUsdt())
            );
        }

        private static BigDecimal amount(String value) {
            return new BigDecimal(value);
        }

        private static void assertAmountEquals(String expected, BigDecimal actual) {
            assertEquals(0, actual.compareTo(amount(expected)));
        }
    }

    private static final class FailingOrderRepository implements OrderRepository {

        @Override
        public Order save(Order order) {
            throw new RuntimeException("order save failed");
        }

        @Override
        public Optional<Order> findById(String id) {
            return Optional.empty();
        }

        @Override
        public Optional<Order> findByUserIdAndClientOrderId(String userId, String clientOrderId) {
            return Optional.empty();
        }

        @Override
        public void deleteById(String id) {
        }

        @Override
        public List<Order> findAll() {
            return List.of();
        }
    }

    private static final class FailingUserBalanceRepository implements UserBalanceRepository {

        private final UserBalance storedBalance;

        private FailingUserBalanceRepository(String userId, String availableUsdt, String reservedUsdt) {
            this.storedBalance = new UserBalance(userId, new BigDecimal(availableUsdt), new BigDecimal(reservedUsdt));
        }

        @Override
        public Optional<UserBalance> findByUserId(String userId) {
            return Optional.of(storedBalance.copy());
        }

        @Override
        public UserBalance save(UserBalance balance) {
            throw new RuntimeException("balance save failed");
        }
    }

    private static final class FailingCancelledOrderRepository implements OrderRepository {

        private final InMemoryOrderRepository delegate = new InMemoryOrderRepository();

        @Override
        public Order save(Order order) {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new RuntimeException("order save failed");
            }
            return delegate.save(order);
        }

        @Override
        public Optional<Order> findById(String id) {
            return delegate.findById(id);
        }

        @Override
        public Optional<Order> findByUserIdAndClientOrderId(String userId, String clientOrderId) {
            return delegate.findByUserIdAndClientOrderId(userId, clientOrderId);
        }

        @Override
        public void deleteById(String id) {
            delegate.deleteById(id);
        }

        @Override
        public List<Order> findAll() {
            return delegate.findAll();
        }
    }

    private static final class FailNextBalanceSaveRepository implements UserBalanceRepository {

        private final InMemoryUserBalanceRepository delegate = new InMemoryUserBalanceRepository();
        private boolean failNextSave;

        void failNextSave() {
            this.failNextSave = true;
        }

        @Override
        public Optional<UserBalance> findByUserId(String userId) {
            return delegate.findByUserId(userId);
        }

        @Override
        public UserBalance save(UserBalance balance) {
            if (failNextSave) {
                failNextSave = false;
                throw new RuntimeException("balance save failed");
            }
            return delegate.save(balance);
        }
    }
}
