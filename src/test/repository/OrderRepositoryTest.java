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
@ActiveProfiles("e2e")
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
    private Player player1;

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
        User user2 = userRepository.save(User.builder()
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
        Player player2 = playerRepository.save(Player.builder()
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
}
