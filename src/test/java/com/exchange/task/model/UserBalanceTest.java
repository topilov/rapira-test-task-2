package com.exchange.task.model;

import com.exchange.task.exception.BalanceStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserBalanceTest {

    @Test
    void release_rejects_amount_above_reserved_balance() {
        UserBalance balance = new UserBalance("user-1", new BigDecimal("1000.00"), new BigDecimal("10.00"));

        BalanceStateException exception = assertThrows(BalanceStateException.class,
                () -> balance.release(new BigDecimal("20.00")));

        assertEquals("reserved balance is insufficient", exception.getMessage());
    }
}
