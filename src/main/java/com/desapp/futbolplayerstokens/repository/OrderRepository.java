package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdempotencyKey(String key);
    List<Order> findByUserId(Long userId);
    List<Order> findByUserIdAndPlayerId(Long userId, Long playerId);
}

