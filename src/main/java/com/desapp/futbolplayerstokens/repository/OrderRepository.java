package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdempotencyKey(String key);
    List<Order> findByUser(User user);
    List<Order> findByUserAndPlayer(User user, Player player);

    @Query("SELECT o FROM Order o WHERE o.player = :player AND o.type = 'SELL' AND o.status = 'PENDING' AND o.priceAtOrder <= :maxPrice ORDER BY o.priceAtOrder ASC, o.createdAt ASC")
    List<Order> findPendingSellOrdersForBuy(@Param("player") Player player, @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT o FROM Order o WHERE o.player = :player AND o.type = 'BUY' AND o.status = 'PENDING' AND o.priceAtOrder >= :minPrice ORDER BY o.priceAtOrder DESC, o.createdAt ASC")
    List<Order> findPendingBuyOrdersForSell(@Param("player") Player player, @Param("minPrice") BigDecimal minPrice);

    List<Order> findByUserAndStatus(User user, Order.OrderStatus status);

    List<Order> findByStatus(Order.OrderStatus status);
}

