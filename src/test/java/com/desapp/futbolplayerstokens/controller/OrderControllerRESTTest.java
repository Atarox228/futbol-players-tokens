package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerRESTTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderControllerREST orderController;

    @BeforeEach
    void setUp() {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("testuser");
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        lenient().when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(User.builder().id(1L).username("testuser").build()));
    }

    @Test
    void buy_createsOrder() {
        OrderControllerREST.BuyRequest req = new OrderControllerREST.BuyRequest(10L, 5, "key-1", new BigDecimal("100"));
        OrderDTO expected = OrderDTO.builder().id(1L).build();
        when(orderService.buy(1L, 10L, 5, "key-1", new BigDecimal("100"))).thenReturn(expected);

        OrderDTO result = orderController.buy(req);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void sell_createsOrder() {
        OrderControllerREST.SellRequest req = new OrderControllerREST.SellRequest(10L, 3, "key-2", new BigDecimal("80"));
        OrderDTO expected = OrderDTO.builder().id(2L).build();
        when(orderService.sell(1L, 10L, 3, "key-2", new BigDecimal("80"))).thenReturn(expected);

        OrderDTO result = orderController.sell(req);

        assertNotNull(result);
        assertEquals(2L, result.getId());
    }

    @Test
    void transactions_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<OrderDTO> page = new PageImpl<>(List.of(OrderDTO.builder().id(1L).build()));
        when(orderService.getTransactionsByUserId(1L, pageable)).thenReturn(page);

        Page<OrderDTO> result = orderController.transactions(pageable);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void orderBook_withType() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<OrderDTO> page = new PageImpl<>(List.of());
        when(orderService.getOrderBook(eq(Order.OrderType.BUY), any(Pageable.class))).thenReturn(page);

        Page<OrderDTO> result = orderController.orderBook("BUY", pageable);

        assertTrue(result.getContent().isEmpty());
        verify(orderService).getOrderBook(eq(Order.OrderType.BUY), any(Pageable.class));
    }

    @Test
    void orderBook_withoutType() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<OrderDTO> page = new PageImpl<>(List.of());
        when(orderService.getOrderBook(eq(null), any(Pageable.class))).thenReturn(page);

        Page<OrderDTO> result = orderController.orderBook(null, pageable);

        assertTrue(result.getContent().isEmpty());
        verify(orderService).getOrderBook(eq(null), any(Pageable.class));
    }

    @Test
    void pendingOrders_withType() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<OrderDTO> page = new PageImpl<>(List.of());
        when(orderService.getPendingOrdersByUserId(1L, Order.OrderType.SELL, pageable)).thenReturn(page);

        Page<OrderDTO> result = orderController.pendingOrders("SELL", pageable);

        assertTrue(result.getContent().isEmpty());
        verify(orderService).getPendingOrdersByUserId(1L, Order.OrderType.SELL, pageable);
    }

    @Test
    void pendingOrders_withoutType() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<OrderDTO> page = new PageImpl<>(List.of());
        when(orderService.getPendingOrdersByUserId(1L, null, pageable)).thenReturn(page);

        Page<OrderDTO> result = orderController.pendingOrders(null, pageable);

        assertTrue(result.getContent().isEmpty());
        verify(orderService).getPendingOrdersByUserId(1L, null, pageable);
    }

    @Test
    void sellAll_sellsAllTokens() {
        String today = LocalDate.of(2026, 6, 9).toString();
        when(orderService.sellAll(1L, "sell-all-" + today)).thenReturn(List.of());

        List<OrderDTO> result = orderController.sellAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderService).sellAll(1L, "sell-all-" + today);
    }

    @Test
    void ordersByPlayer_returnsOrders() {
        when(orderService.getOrdersByPlayer(10L)).thenReturn(List.of(OrderDTO.builder().id(1L).build()));

        List<OrderDTO> result = orderController.ordersByPlayer(10L);

        assertEquals(1, result.size());
        verify(orderService).getOrdersByPlayer(10L);
    }

    @Test
    void cancelOrder_cancelsOrder() {
        when(orderService.cancelOrder(1L, 5L)).thenReturn(OrderDTO.builder().id(5L).status("CANCELLED").build());

        OrderDTO result = orderController.cancelOrder(5L);

        assertEquals("CANCELLED", result.getStatus());
    }
}
