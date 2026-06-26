package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdempotencyKey(String key);
    List<Order> findByUser(User user);
    List<Order> findByUserAndPlayer(User user, Player player);

    @Query("SELECT o FROM Order o WHERE o.player = :player AND o.type = 'SELL' AND o.status IN ('PENDING', 'PARTIALLY_FILLED') AND o.priceAtOrder <= :maxPrice ORDER BY o.priceAtOrder ASC, o.createdAt ASC")
    List<Order> findPendingSellOrdersForBuy(@Param("player") Player player, @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT o FROM Order o WHERE o.player = :player AND o.type = 'BUY' AND o.status IN ('PENDING', 'PARTIALLY_FILLED') AND o.priceAtOrder >= :minPrice ORDER BY o.priceAtOrder DESC, o.createdAt ASC")
    List<Order> findPendingBuyOrdersForSell(@Param("player") Player player, @Param("minPrice") BigDecimal minPrice);

    List<Order> findByUserAndStatus(User user, Order.OrderStatus status);
    Page<Order> findByUserAndStatus(User user, Order.OrderStatus status, Pageable pageable);

    List<Order> findByStatus(Order.OrderStatus status);
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    List<Order> findByStatusAndType(Order.OrderStatus status, Order.OrderType type);
    Page<Order> findByStatusAndType(Order.OrderStatus status, Order.OrderType type, Pageable pageable);

    List<Order> findByUserAndStatusAndType(User user, Order.OrderStatus status, Order.OrderType type);
    Page<Order> findByUserAndStatusAndType(User user, Order.OrderStatus status, Order.OrderType type, Pageable pageable);

    Page<Order> findByUser(User user, Pageable pageable);
    List<Order> findByPlayerAndStatusIn(Player player, Collection<Order.OrderStatus> statuses);

    long countByStatusAndType(Order.OrderStatus status, Order.OrderType type);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status = :status AND o.type = :type")
    BigDecimal sumTotalByStatusAndType(@Param("status") Order.OrderStatus status, @Param("type") Order.OrderType type);

    @Query("SELECT o.player.id, COUNT(o), SUM(o.quantity), SUM(o.total) FROM Order o WHERE o.status IN ('FILLED', 'PARTIALLY_FILLED') GROUP BY o.player.id ORDER BY COUNT(o) DESC")
    List<Object[]> findTopTradedPlayers();

    @Query("SELECT COUNT(DISTINCT o.player.id) FROM Order o WHERE o.status IN ('FILLED', 'PARTIALLY_FILLED') AND o.createdAt BETWEEN :start AND :end")
    long countDistinctPlayersTradedBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}

