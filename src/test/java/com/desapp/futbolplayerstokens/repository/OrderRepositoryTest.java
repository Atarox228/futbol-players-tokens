package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Order;
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

    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        order1 = Order.builder()
                .userId(1L)
                .playerId(10L)
                .type(Order.OrderType.BUY)
                .quantity(5)
                .priceAtOrder(new BigDecimal("1.5"))
                .total(new BigDecimal("7.5"))
                .idempotencyKey("key-1")
                .build();

        order2 = Order.builder()
                .userId(2L)
                .playerId(11L)
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
    void testFindByUserId() {
        orderRepository.save(order1);
        orderRepository.save(order2);

        List<Order> orders = orderRepository.findByUserId(1L);
        assertEquals(1, orders.size());
        assertEquals(10L, orders.get(0).getPlayerId());
    }
}

