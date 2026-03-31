package com.exchange.task.repository;

import com.exchange.task.model.UserBalance;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserBalanceRepository implements UserBalanceRepository {

    private final Map<String, UserBalance> storage = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        storage.put("user-1", new UserBalance("user-1", new BigDecimal("1000.00"), BigDecimal.ZERO));
        storage.put("user-2", new UserBalance("user-2", new BigDecimal("50.00"), BigDecimal.ZERO));
    }

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
