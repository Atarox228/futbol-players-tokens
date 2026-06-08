package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    private Order order1;
    private Order order2;
    private User user1;
    private User user2;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .username("user-order-1")
                .email("user-order-1@example.com")
                .password("password")
                .role(User.Role.USER)
                .balance(new BigDecimal("1000"))
                .build());
        user2 = userRepository.save(User.builder()
                .username("user-order-2")
                .email("user-order-2@example.com")
                .password("password")
                .role(User.Role.USER)
                .balance(new BigDecimal("1000"))
                .build());

        player1 = playerRepository.save(Player.builder()
                .name("Player Order 1")
                .team("Team A")
                .league("League A")
                .position("Forward")
                .build());
        player2 = playerRepository.save(Player.builder()
                .name("Player Order 2")
                .team("Team B")
                .league("League B")
                .position("Midfielder")
                .build());

        order1 = Order.builder()
                .user(user1)
                .player(player1)
                .type(Order.OrderType.BUY)
                .quantity(5)
                .priceAtOrder(new BigDecimal("1.5"))
                .total(new BigDecimal("7.5"))
                .idempotencyKey("key-1")
                .build();

        order2 = Order.builder()
                .user(user2)
                .player(player2)
                .type(Order.OrderType.SELL)
                .quantity(2)
                .priceAtOrder(new BigDecimal("2.0"))
                .total(new BigDecimal("4.0"))
                .idempotencyKey("key-2")
                .build();
    }

    @Test
    void testFindByIdempotencyKey() {
        Order saved = orderRepository.save(order1);

        Optional<Order> found = orderRepository.findByIdempotencyKey("key-1");

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void testFindByUser() {
        orderRepository.save(order1);
        orderRepository.save(order2);

        List<Order> orders = orderRepository.findByUser(user1);
        assertEquals(1, orders.size());
        assertEquals(player1.getId(), orders.getFirst().getPlayer().getId());
    }

    @Test
    void findPendingSellOrdersForBuy_filtersByPrice() {
        Order sellLow = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.SELL)
                .quantity(1).priceAtOrder(new BigDecimal("10")).total(new BigDecimal("10"))
                .idempotencyKey("sell-low").build());
        Order sellMid = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.SELL)
                .quantity(1).priceAtOrder(new BigDecimal("20")).total(new BigDecimal("20"))
                .idempotencyKey("sell-mid").build());
        orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.SELL)
                .quantity(1).priceAtOrder(new BigDecimal("30")).total(new BigDecimal("30"))
                .idempotencyKey("sell-high").build());

        List<Order> result = orderRepository.findPendingSellOrdersForBuy(player1, new BigDecimal("25"));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(sellLow.getId())));
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(sellMid.getId())));
    }

    @Test
    void findPendingSellOrdersForBuy_includesPartiallyFilled() {
        Order pending = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.SELL)
                .quantity(5).priceAtOrder(new BigDecimal("10")).total(new BigDecimal("50"))
                .idempotencyKey("sell-pending").build());
        Order partiallyFilled = orderRepository.save(Order.builder()
                .user(user2).player(player1).type(Order.OrderType.SELL)
                .quantity(5).priceAtOrder(new BigDecimal("10")).total(new BigDecimal("50"))
                .remainingQuantity(3)
                .status(Order.OrderStatus.PARTIALLY_FILLED)
                .idempotencyKey("sell-partial").build());

        List<Order> result = orderRepository.findPendingSellOrdersForBuy(player1, new BigDecimal("15"));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(pending.getId())));
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(partiallyFilled.getId())));
    }

    @Test
    void findPendingSellOrdersForBuy_ordersByPriceAsc() {
        Order sell30 = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.SELL)
                .quantity(1).priceAtOrder(new BigDecimal("30")).total(new BigDecimal("30"))
                .idempotencyKey("sell-30").build());
        Order sell10 = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.SELL)
                .quantity(1).priceAtOrder(new BigDecimal("10")).total(new BigDecimal("10"))
                .idempotencyKey("sell-10").build());
        Order sell20 = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.SELL)
                .quantity(1).priceAtOrder(new BigDecimal("20")).total(new BigDecimal("20"))
                .idempotencyKey("sell-20").build());

        List<Order> result = orderRepository.findPendingSellOrdersForBuy(player1, new BigDecimal("50"));

        assertEquals(3, result.size());
        assertEquals(sell10.getId(), result.get(0).getId());
        assertEquals(sell20.getId(), result.get(1).getId());
        assertEquals(sell30.getId(), result.get(2).getId());
    }

    @Test
    void findPendingBuyOrdersForSell_filtersByPrice() {
        Order buyHigh = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.BUY)
                .quantity(1).priceAtOrder(new BigDecimal("30")).total(new BigDecimal("30"))
                .idempotencyKey("buy-high").build());
        Order buyMid = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.BUY)
                .quantity(1).priceAtOrder(new BigDecimal("20")).total(new BigDecimal("20"))
                .idempotencyKey("buy-mid").build());
        orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.BUY)
                .quantity(1).priceAtOrder(new BigDecimal("10")).total(new BigDecimal("10"))
                .idempotencyKey("buy-low").build());

        List<Order> result = orderRepository.findPendingBuyOrdersForSell(player1, new BigDecimal("15"));

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(buyHigh.getId())));
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(buyMid.getId())));
    }

    @Test
    void findPendingBuyOrdersForSell_ordersByPriceDesc() {
        Order buy10 = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.BUY)
                .quantity(1).priceAtOrder(new BigDecimal("10")).total(new BigDecimal("10"))
                .idempotencyKey("buy-low-2").build());
        Order buy30 = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.BUY)
                .quantity(1).priceAtOrder(new BigDecimal("30")).total(new BigDecimal("30"))
                .idempotencyKey("buy-high-2").build());
        Order buy20 = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.BUY)
                .quantity(1).priceAtOrder(new BigDecimal("20")).total(new BigDecimal("20"))
                .idempotencyKey("buy-mid-2").build());

        List<Order> result = orderRepository.findPendingBuyOrdersForSell(player1, BigDecimal.ZERO);

        assertEquals(3, result.size());
        assertEquals(buy30.getId(), result.get(0).getId());
        assertEquals(buy20.getId(), result.get(1).getId());
        assertEquals(buy10.getId(), result.get(2).getId());
    }

    @Test
    void findByPlayerAndStatusIn_returnsCorrectOrders() {
        Order buyPending = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.BUY)
                .quantity(1).priceAtOrder(new BigDecimal("10")).total(new BigDecimal("10"))
                .idempotencyKey("bp1").build());
        Order sellPendingP1 = orderRepository.save(Order.builder()
                .user(user2).player(player1).type(Order.OrderType.SELL)
                .quantity(1).priceAtOrder(new BigDecimal("10")).total(new BigDecimal("10"))
                .idempotencyKey("sp1").build());
        Order sellPartiallyP1 = orderRepository.save(Order.builder()
                .user(user1).player(player1).type(Order.OrderType.SELL)
                .quantity(5).priceAtOrder(new BigDecimal("10")).total(new BigDecimal("50"))
                .remainingQuantity(3)
                .status(Order.OrderStatus.PARTIALLY_FILLED)
                .idempotencyKey("spp1").build());
        orderRepository.save(Order.builder()
                .user(user2).player(player2).type(Order.OrderType.BUY)
                .quantity(1).priceAtOrder(new BigDecimal("10")).total(new BigDecimal("10"))
                .idempotencyKey("bp2").build());
        orderRepository.save(Order.builder()
                .user(user2).player(player1).type(Order.OrderType.SELL)
                .quantity(1).priceAtOrder(new BigDecimal("10")).total(new BigDecimal("10"))
                .status(Order.OrderStatus.FILLED).remainingQuantity(0)
                .idempotencyKey("sf1").build());
        orderRepository.save(Order.builder()
                .user(user2).player(player1).type(Order.OrderType.SELL)
                .quantity(1).priceAtOrder(new BigDecimal("10")).total(new BigDecimal("10"))
                .status(Order.OrderStatus.CANCELLED).remainingQuantity(0)
                .idempotencyKey("sc1").build());

        List<Order> result = orderRepository.findByPlayerAndStatusIn(player1,
                List.of(Order.OrderStatus.PENDING, Order.OrderStatus.PARTIALLY_FILLED));

        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(buyPending.getId())));
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(sellPendingP1.getId())));
        assertTrue(result.stream().anyMatch(o -> o.getId().equals(sellPartiallyP1.getId())));
    }
}
