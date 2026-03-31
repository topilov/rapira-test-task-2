package com.exchange.task.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.exchange.task.model.Order;
import com.exchange.task.model.OrderStatus;
import com.exchange.task.model.UserBalance;
import com.exchange.task.repository.InMemoryOrderRepository;
import com.exchange.task.repository.InMemoryUserBalanceRepository;
import com.exchange.task.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayNameGeneration(ReplaceUnderscores.class)
class OrderControllerIntegrationTest {

    private static final String USER_ID = "user-1";
    private static final String SECOND_USER_ID = "user-2";
    private static final String CLIENT_ORDER_ID = "client-1";
    private static final String SYMBOL = "BTCUSDT";

    private OrderApiDriver api;

    @BeforeEach
    void setUp() {
        api = new OrderApiDriver();
    }

    @Nested
    class CreateOrder {

        @Test
        void returns_created_order_in_new_status() throws Exception {
            ApiResponse<OrderResponse> response = api.createOrder(request(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "2.00"));

            assertAll(
                    () -> assertEquals(HttpStatus.CREATED.value(), response.status()),
                    () -> assertOrderResponse(response.body(), CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "2.00", OrderStatus.NEW)
            );
        }

        @Test
        void returns_bad_request_for_invalid_payload() throws Exception {
            ApiResponse<ApiErrorResponse> response = api.createOrderForError(request(" ", USER_ID, SYMBOL, "100.00", "1.00"));

            assertAll(
                    () -> assertEquals(HttpStatus.BAD_REQUEST.value(), response.status()),
                    () -> assertEquals("clientOrderId is blank", response.body().message())
            );
        }

        @Test
        void returns_conflict_when_balance_is_insufficient() throws Exception {
            ApiResponse<ApiErrorResponse> response = api.createOrderForError(request(CLIENT_ORDER_ID, SECOND_USER_ID, SYMBOL, "100.00", "1.00"));

            assertAll(
                    () -> assertEquals(HttpStatus.CONFLICT.value(), response.status()),
                    () -> assertEquals("insufficient balance", response.body().message())
            );
        }

        @Test
        void returns_conflict_when_idempotency_key_is_reused_with_different_payload() throws Exception {
            api.createOrder(request(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00"));

            ApiResponse<ApiErrorResponse> response = api.createOrderForError(request(CLIENT_ORDER_ID, USER_ID, SYMBOL, "101.00", "1.00"));

            assertAll(
                    () -> assertEquals(HttpStatus.CONFLICT.value(), response.status()),
                    () -> assertEquals("clientOrderId already exists with different payload", response.body().message())
            );
        }

        @Test
        void returns_ok_for_idempotent_replay() throws Exception {
            OrderResponse created = api.createOrder(request(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00")).body();

            ApiResponse<OrderResponse> response = api.createOrder(request(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00"));

            assertAll(
                    () -> assertEquals(HttpStatus.OK.value(), response.status()),
                    () -> assertEquals(created.id(), response.body().id()),
                    () -> assertOrderResponse(response.body(), CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00", OrderStatus.NEW)
            );
        }
    }

    @Nested
    class CancelOrder {

        @Test
        void returns_cancelled_order() throws Exception {
            String orderId = api.createOrder(request(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00")).body().id();

            ApiResponse<OrderResponse> response = api.cancelOrder(orderId);

            assertAll(
                    () -> assertEquals(HttpStatus.OK.value(), response.status()),
                    () -> assertEquals(orderId, response.body().id()),
                    () -> assertOrderResponse(response.body(), CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00", OrderStatus.CANCELLED)
            );
        }

        @Test
        void returns_not_found_for_unknown_order() throws Exception {
            ApiResponse<ApiErrorResponse> response = api.cancelOrderForError("missing-order");

            assertAll(
                    () -> assertEquals(HttpStatus.NOT_FOUND.value(), response.status()),
                    () -> assertEquals("order not found", response.body().message())
            );
        }

        @Test
        void returns_conflict_for_repeated_cancellation() throws Exception {
            String orderId = api.createOrder(request(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00")).body().id();
            api.cancelOrder(orderId);

            ApiResponse<ApiErrorResponse> response = api.cancelOrderForError(orderId);

            assertAll(
                    () -> assertEquals(HttpStatus.CONFLICT.value(), response.status()),
                    () -> assertEquals("only NEW order can be cancelled", response.body().message())
            );
        }

        @Test
        void returns_conflict_for_inconsistent_reserved_balance() throws Exception {
            Order order = api.givenNewOrder(CLIENT_ORDER_ID, USER_ID, SYMBOL, "100.00", "1.00");

            ApiResponse<ApiErrorResponse> response = api.cancelOrderForError(order.getId());

            assertAll(
                    () -> assertEquals(HttpStatus.CONFLICT.value(), response.status()),
                    () -> assertEquals("reserved balance is insufficient", response.body().message())
            );
        }
    }

    private CreateOrderRequest request(String clientOrderId, String userId, String symbol, String price, String quantity) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setClientOrderId(clientOrderId);
        request.setUserId(userId);
        request.setSymbol(symbol);
        request.setPrice(new BigDecimal(price));
        request.setQuantity(new BigDecimal(quantity));
        return request;
    }

    private void assertOrderResponse(OrderResponse response,
                                     String expectedClientOrderId,
                                     String expectedUserId,
                                     String expectedSymbol,
                                     String expectedPrice,
                                     String expectedQuantity,
                                     OrderStatus expectedStatus) {
        assertAll(
                () -> assertNotNull(response.id()),
                () -> assertEquals(expectedClientOrderId, response.clientOrderId()),
                () -> assertEquals(expectedUserId, response.userId()),
                () -> assertEquals(expectedSymbol, response.symbol()),
                () -> assertEquals(0, response.price().compareTo(new BigDecimal(expectedPrice))),
                () -> assertEquals(0, response.quantity().compareTo(new BigDecimal(expectedQuantity))),
                () -> assertEquals(expectedStatus, response.status())
        );
    }

    private static final class OrderApiDriver {

        private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        private final InMemoryOrderRepository orderRepository;
        private final InMemoryUserBalanceRepository balanceRepository;
        private final MockMvc mockMvc;

        private OrderApiDriver() {
            orderRepository = new InMemoryOrderRepository();
            balanceRepository = new InMemoryUserBalanceRepository();
            balanceRepository.save(new UserBalance(USER_ID, new BigDecimal("1000.00"), BigDecimal.ZERO));
            balanceRepository.save(new UserBalance(SECOND_USER_ID, new BigDecimal("50.00"), BigDecimal.ZERO));

            OrderService orderService = new OrderService(orderRepository, balanceRepository);
            mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                    .setControllerAdvice(new ApiExceptionHandler())
                    .build();
        }

        ApiResponse<OrderResponse> createOrder(CreateOrderRequest request) throws Exception {
            return execute(post("/api/orders"), request, OrderResponse.class);
        }

        ApiResponse<ApiErrorResponse> createOrderForError(CreateOrderRequest request) throws Exception {
            return execute(post("/api/orders"), request, ApiErrorResponse.class);
        }

        ApiResponse<OrderResponse> cancelOrder(String orderId) throws Exception {
            return execute(post("/api/orders/{orderId}/cancel", orderId), null, OrderResponse.class);
        }

        ApiResponse<ApiErrorResponse> cancelOrderForError(String orderId) throws Exception {
            return execute(post("/api/orders/{orderId}/cancel", orderId), null, ApiErrorResponse.class);
        }

        Order givenNewOrder(String clientOrderId, String userId, String symbol, String price, String quantity) {
            return orderRepository.save(new Order(
                    clientOrderId,
                    userId,
                    symbol,
                    new BigDecimal(price),
                    new BigDecimal(quantity)
            ));
        }

        private <T> ApiResponse<T> execute(MockHttpServletRequestBuilder requestBuilder, Object requestBody, Class<T> responseType)
                throws Exception {
            if (requestBody != null) {
                requestBuilder.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(requestBody));
            }

            MvcResult result = mockMvc.perform(requestBuilder).andReturn();
            String content = result.getResponse().getContentAsString();
            T body = content.isBlank() ? null : objectMapper.readValue(content, responseType);
            return new ApiResponse<>(result.getResponse().getStatus(), body);
        }
    }

    private record ApiResponse<T>(int status, T body) {
    }
}
