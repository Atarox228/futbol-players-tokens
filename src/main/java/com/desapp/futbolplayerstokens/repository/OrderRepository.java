package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdempotencyKey(String key);
    List<Order> findByUser(User user);
    List<Order> findByUserAndPlayer(User user, Player player);
}

