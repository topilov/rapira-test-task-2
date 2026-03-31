package com.exchange.task.repository;

import com.exchange.task.model.UserBalance;

import java.util.Optional;

public interface UserBalanceRepository {
    Optional<UserBalance> findByUserId(String userId);
    UserBalance save(UserBalance balance);
}
