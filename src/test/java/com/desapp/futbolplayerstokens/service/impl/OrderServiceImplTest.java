package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.exception.InsufficientBalanceException;
import com.desapp.futbolplayerstokens.exception.InsufficientTokensException;
import com.desapp.futbolplayerstokens.exception.ResourceNotFoundException;
import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.OrderRepository;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.service.PortfolioService;
import com.desapp.futbolplayerstokens.service.QuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private QuoteService quoteService;

    private OrderServiceImpl orderService;
    private User user;
    private Player player;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, portfolioRepository, playerRepository,
                userRepository, portfolioService, quoteService);

        user = User.builder()
                .id(1L)
                .username("user")
                .email("user@example.com")
                .password("password")
                .role(User.Role.USER)
                .balance(new BigDecimal("5000"))
                .build();
        player = Player.builder()
                .id(10L)
                .name("Messi")
                .availableTokens(10)
                .totalTokens(100)
                .build();
    }

    @Test
    void buy_success() {
        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(quoteService.getCurrentQuote(10L)).thenReturn(QuoteDTO.builder().price(new BigDecimal("100")).build());
        when(orderRepository.findPendingSellOrdersForBuy(any(), any())).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            order.setStatus(Order.OrderStatus.PENDING);
            return order;
        });
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        OrderDTO result = orderService.buy(1L, 10L, 3, "key", null);

        assertEquals(99L, result.getId());
        assertEquals(new BigDecimal("4700"), user.getBalance());
    }

    @Test
    void buy_insufficientBalance() {
        user.setBalance(new BigDecimal("100"));
        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(quoteService.getCurrentQuote(10L)).thenReturn(QuoteDTO.builder().price(new BigDecimal("200")).build());

        assertThrows(InsufficientBalanceException.class, () -> orderService.buy(1L, 10L, 1, "key", null));

        verify(orderRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void buy_idempotency() {
        Order existingOrder = Order.builder()
                .id(55L)
                .user(user)
                .player(player)
                .type(Order.OrderType.BUY)
                .quantity(3)
                .priceAtOrder(new BigDecimal("100"))
                .total(new BigDecimal("300"))
                .idempotencyKey("key")
                .status(Order.OrderStatus.PENDING)
                .remainingQuantity(3)
                .build();
        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.of(existingOrder));

        OrderDTO result = orderService.buy(1L, 10L, 3, "key", null);

        assertEquals(55L, result.getId());
        verifyNoInteractions(userRepository, playerRepository);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void sell_success() {
        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .player(player)
                .tokenQty(5)
                .avgBuyPrice(new BigDecimal("80"))
                .build();
        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(portfolioRepository.findByUserAndPlayer(user, player)).thenReturn(Optional.of(portfolio));
        when(quoteService.getCurrentQuote(10L)).thenReturn(QuoteDTO.builder().price(new BigDecimal("100")).build());
        when(orderRepository.findPendingBuyOrdersForSell(any(), any())).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            order.setStatus(Order.OrderStatus.PENDING);
            return order;
        });
        when(orderRepository.findById(100L)).thenReturn(Optional.empty());

        OrderDTO result = orderService.sell(1L, 10L, 2, "key", null);

        assertEquals(100L, result.getId());
        verify(portfolioService).updatePosition(1L, 10L, 2, new BigDecimal("100"), Order.OrderType.SELL);
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void sell_insufficientTokens() {
        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .player(player)
                .tokenQty(1)
                .avgBuyPrice(new BigDecimal("80"))
                .build();
        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(portfolioRepository.findByUserAndPlayer(user, player)).thenReturn(Optional.of(portfolio));
        when(quoteService.getCurrentQuote(10L)).thenReturn(QuoteDTO.builder().price(new BigDecimal("100")).build());

        assertThrows(InsufficientTokensException.class, () -> orderService.sell(1L, 10L, 3, "key", null));

        verify(orderRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void sell_noPortfolio() {
        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(portfolioRepository.findByUserAndPlayer(user, player)).thenReturn(Optional.empty());
        when(quoteService.getCurrentQuote(10L)).thenReturn(QuoteDTO.builder().price(new BigDecimal("100")).build());

        assertThrows(ResourceNotFoundException.class, () -> orderService.sell(1L, 10L, 3, "key", null));

        verify(orderRepository, never()).save(any());
    }
}
