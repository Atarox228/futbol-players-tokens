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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
                userRepository, portfolioService, quoteService, new SimpleMeterRegistry());

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

    @Test
    void cancelOrder_buyPending() {
        Order buyOrder = Order.builder()
                .id(50L).user(user).player(player).type(Order.OrderType.BUY)
                .quantity(5).priceAtOrder(new BigDecimal("100")).total(new BigDecimal("500"))
                .idempotencyKey("buy-key").status(Order.OrderStatus.PENDING).remainingQuantity(5)
                .build();
        when(orderRepository.findById(50L)).thenReturn(Optional.of(buyOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.cancelOrder(1L, 50L);

        assertEquals(Order.OrderStatus.CANCELLED.name(), result.getStatus());
        assertEquals(0, result.getRemainingQuantity());
        verify(userRepository).save(user);
        assertEquals(new BigDecimal("5500"), user.getBalance());
    }

    @Test
    void cancelOrder_sellPending() {
        Order sellOrder = Order.builder()
                .id(51L).user(user).player(player).type(Order.OrderType.SELL)
                .quantity(5).priceAtOrder(new BigDecimal("100")).total(new BigDecimal("500"))
                .idempotencyKey("sell-key").status(Order.OrderStatus.PENDING).remainingQuantity(5)
                .build();
        when(orderRepository.findById(51L)).thenReturn(Optional.of(sellOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.cancelOrder(1L, 51L);

        assertEquals(Order.OrderStatus.CANCELLED.name(), result.getStatus());
        verifyNoInteractions(portfolioService);
    }

    @Test
    void cancelOrder_sellPartiallyFilled() {
        Order sellOrder = Order.builder()
                .id(52L).user(user).player(player).type(Order.OrderType.SELL)
                .quantity(5).priceAtOrder(new BigDecimal("100")).total(new BigDecimal("500"))
                .idempotencyKey("sell-partial-key").status(Order.OrderStatus.PARTIALLY_FILLED).remainingQuantity(3)
                .build();
        when(orderRepository.findById(52L)).thenReturn(Optional.of(sellOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDTO result = orderService.cancelOrder(1L, 52L);

        assertEquals(Order.OrderStatus.CANCELLED.name(), result.getStatus());
        verifyNoInteractions(portfolioService);
    }

    @Test
    void cancelOrder_filled_throws() {
        Order filledOrder = Order.builder()
                .id(53L).user(user).player(player).type(Order.OrderType.SELL)
                .quantity(5).priceAtOrder(new BigDecimal("100")).total(new BigDecimal("500"))
                .idempotencyKey("filled-key").status(Order.OrderStatus.FILLED).remainingQuantity(0)
                .build();
        when(orderRepository.findById(53L)).thenReturn(Optional.of(filledOrder));

        assertThrows(IllegalArgumentException.class, () -> orderService.cancelOrder(1L, 53L));
    }

    @Test
    void matchBuyOrder() {
        User seller = User.builder().id(2L).username("seller").email("seller@test.com")
                .password("pass").role(User.Role.USER).balance(new BigDecimal("3000"))
                .build();
        Order sellOrder = Order.builder()
                .id(200L).user(seller).player(player).type(Order.OrderType.SELL)
                .quantity(5).priceAtOrder(new BigDecimal("100")).total(new BigDecimal("500"))
                .idempotencyKey("sell-match-key").status(Order.OrderStatus.PENDING).remainingQuantity(5)
                .build();

        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(orderRepository.findPendingSellOrdersForBuy(player, new BigDecimal("100")))
                .thenReturn(List.of(sellOrder));

        final Map<Long, Order> savedOrderMap = new HashMap<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) o.setId(99L + savedOrderMap.size());
            savedOrderMap.put(o.getId(), o);
            return o;
        });
        when(orderRepository.findById(anyLong())).thenAnswer(i ->
                Optional.ofNullable(savedOrderMap.get(i.getArgument(0)))
        );

        OrderDTO result = orderService.buy(1L, 10L, 10, "key", new BigDecimal("100"));

        assertEquals("PARTIALLY_FILLED", result.getStatus());
        assertEquals(5, result.getRemainingQuantity());
        verify(portfolioService).transferTokens(2L, 1L, 10L, 5, new BigDecimal("100"));
        verify(userRepository, atLeastOnce()).save(seller);
    }

    @Test
    void matchBuyOrder_multipleSellOrders() {
        User seller = User.builder().id(2L).username("seller").email("seller@test.com")
                .password("pass").role(User.Role.USER).balance(new BigDecimal("3000"))
                .build();
        Order sellCheap = Order.builder()
                .id(210L).user(seller).player(player).type(Order.OrderType.SELL)
                .quantity(3).priceAtOrder(new BigDecimal("90")).total(new BigDecimal("270"))
                .idempotencyKey("sell-cheap").status(Order.OrderStatus.PENDING).remainingQuantity(3)
                .build();
        Order sellMid = Order.builder()
                .id(211L).user(seller).player(player).type(Order.OrderType.SELL)
                .quantity(5).priceAtOrder(new BigDecimal("100")).total(new BigDecimal("500"))
                .idempotencyKey("sell-mid").status(Order.OrderStatus.PENDING).remainingQuantity(5)
                .build();
        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        // Only sellCheap (90) and sellMid (100) have price <= 105
        when(orderRepository.findPendingSellOrdersForBuy(player, new BigDecimal("105")))
                .thenReturn(List.of(sellCheap, sellMid));

        final Map<Long, Order> savedOrderMap = new HashMap<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) o.setId(99L + savedOrderMap.size());
            savedOrderMap.put(o.getId(), o);
            return o;
        });
        when(orderRepository.findById(anyLong())).thenAnswer(i ->
                Optional.ofNullable(savedOrderMap.get(i.getArgument(0)))
        );

        OrderDTO result = orderService.buy(1L, 10L, 10, "key", new BigDecimal("105"));

        assertEquals("PARTIALLY_FILLED", result.getStatus());
        assertEquals(2, result.getRemainingQuantity()); // 10 - 3 - 5 = 2
        verify(portfolioService).transferTokens(2L, 1L, 10L, 3, new BigDecimal("90"));
        verify(portfolioService).transferTokens(2L, 1L, 10L, 5, new BigDecimal("100"));
    }

    @Test
    void buy_autoCreatesSuperuserSellOrder() {
        User superuser = User.builder()
                .id(2L).username("superuser").email("super@test.com")
                .password("pass").role(User.Role.SUPERUSER).balance(BigDecimal.ZERO)
                .build();
        Portfolio superPortfolio = Portfolio.builder()
                .user(superuser).player(player).tokenQty(100).avgBuyPrice(BigDecimal.ZERO)
                .build();

        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(quoteService.getCurrentQuote(10L)).thenReturn(QuoteDTO.builder().price(new BigDecimal("100")).build());
        when(userRepository.findByUsername("superuser")).thenReturn(Optional.of(superuser));
        when(portfolioRepository.findByUserAndPlayer(superuser, player)).thenReturn(Optional.of(superPortfolio));

        final Map<Long, Order> savedOrderMap = new HashMap<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) o.setId(99L + savedOrderMap.size());
            savedOrderMap.put(o.getId(), o);
            return o;
        });
        when(orderRepository.findById(anyLong())).thenAnswer(i ->
                Optional.ofNullable(savedOrderMap.get(i.getArgument(0)))
        );

        when(orderRepository.findPendingSellOrdersForBuy(any(), any()))
                .thenReturn(List.of())
                .thenAnswer(i -> savedOrderMap.values().stream()
                        .filter(o -> o.getType() == Order.OrderType.SELL
                                && o.getStatus() == Order.OrderStatus.PENDING
                                && o.getRemainingQuantity() > 0)
                        .collect(Collectors.toList()));

        OrderDTO result = orderService.buy(1L, 10L, 10, "key", null);

        assertEquals("FILLED", result.getStatus());
        assertEquals(0, result.getRemainingQuantity());
        assertEquals(new BigDecimal("4000"), user.getBalance());
        verify(portfolioService).transferTokens(2L, 1L, 10L, 10, new BigDecimal("100"));
    }

    @Test
    void buy_autoSell_partialSuperuserTokens() {
        User superuser = User.builder()
                .id(2L).username("superuser").email("super@test.com")
                .password("pass").role(User.Role.SUPERUSER).balance(BigDecimal.ZERO)
                .build();
        Portfolio superPortfolio = Portfolio.builder()
                .user(superuser).player(player).tokenQty(30).avgBuyPrice(BigDecimal.ZERO)
                .build();

        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(quoteService.getCurrentQuote(10L)).thenReturn(QuoteDTO.builder().price(new BigDecimal("100")).build());
        when(userRepository.findByUsername("superuser")).thenReturn(Optional.of(superuser));
        when(portfolioRepository.findByUserAndPlayer(superuser, player)).thenReturn(Optional.of(superPortfolio));

        final Map<Long, Order> savedOrderMap = new HashMap<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) o.setId(99L + savedOrderMap.size());
            savedOrderMap.put(o.getId(), o);
            return o;
        });
        when(orderRepository.findById(anyLong())).thenAnswer(i ->
                Optional.ofNullable(savedOrderMap.get(i.getArgument(0)))
        );

        when(orderRepository.findPendingSellOrdersForBuy(any(), any()))
                .thenReturn(List.of())
                .thenAnswer(i -> savedOrderMap.values().stream()
                        .filter(o -> o.getType() == Order.OrderType.SELL
                                && o.getStatus() == Order.OrderStatus.PENDING
                                && o.getRemainingQuantity() > 0)
                        .collect(Collectors.toList()));

        OrderDTO result = orderService.buy(1L, 10L, 50, "key", null);

        assertEquals("PARTIALLY_FILLED", result.getStatus());
        assertEquals(20, result.getRemainingQuantity());
        verify(portfolioService).transferTokens(2L, 1L, 10L, 30, new BigDecimal("100"));
    }

    @Test
    void buy_autoSell_withExistingSells() {
        User superuser = User.builder()
                .id(2L).username("superuser").email("super@test.com")
                .password("pass").role(User.Role.SUPERUSER).balance(BigDecimal.ZERO)
                .build();
        User otherSeller = User.builder()
                .id(3L).username("seller").email("seller@test.com")
                .password("pass").role(User.Role.USER).balance(new BigDecimal("3000"))
                .build();
        Portfolio superPortfolio = Portfolio.builder()
                .user(superuser).player(player).tokenQty(100).avgBuyPrice(BigDecimal.ZERO)
                .build();
        Order existingSell = Order.builder()
                .id(200L).user(otherSeller).player(player).type(Order.OrderType.SELL)
                .quantity(5).priceAtOrder(new BigDecimal("100")).total(new BigDecimal("500"))
                .idempotencyKey("existing-sell").status(Order.OrderStatus.PENDING).remainingQuantity(5)
                .build();

        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(quoteService.getCurrentQuote(10L)).thenReturn(QuoteDTO.builder().price(new BigDecimal("100")).build());
        when(userRepository.findByUsername("superuser")).thenReturn(Optional.of(superuser));
        when(portfolioRepository.findByUserAndPlayer(superuser, player)).thenReturn(Optional.of(superPortfolio));

        final Map<Long, Order> savedOrderMap = new HashMap<>();
        savedOrderMap.put(200L, existingSell);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) o.setId(99L + savedOrderMap.size());
            savedOrderMap.put(o.getId(), o);
            return o;
        });
        when(orderRepository.findById(anyLong())).thenAnswer(i ->
                Optional.ofNullable(savedOrderMap.get(i.getArgument(0)))
        );

        when(orderRepository.findPendingSellOrdersForBuy(any(), any()))
                .thenReturn(List.of(existingSell))
                .thenAnswer(i -> savedOrderMap.values().stream()
                        .filter(o -> o.getType() == Order.OrderType.SELL
                                && o.getStatus() == Order.OrderStatus.PENDING
                                && o.getRemainingQuantity() > 0)
                        .collect(Collectors.toList()));

        OrderDTO result = orderService.buy(1L, 10L, 20, "key", null);

        assertEquals("FILLED", result.getStatus());
        assertEquals(0, result.getRemainingQuantity());
        verify(portfolioService).transferTokens(3L, 1L, 10L, 5, new BigDecimal("100"));
        verify(portfolioService).transferTokens(2L, 1L, 10L, 15, new BigDecimal("100"));
    }

    @Test
    void buy_autoSell_quoteExceedsMaxPrice() {
        User superuser = User.builder()
                .id(2L).username("superuser").email("super@test.com")
                .password("pass").role(User.Role.SUPERUSER).balance(BigDecimal.ZERO)
                .build();
        Portfolio superPortfolio = Portfolio.builder()
                .user(superuser).player(player).tokenQty(100).avgBuyPrice(BigDecimal.ZERO)
                .build();

        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(userRepository.findByUsername("superuser")).thenReturn(Optional.of(superuser));
        when(portfolioRepository.findByUserAndPlayer(superuser, player)).thenReturn(Optional.of(superPortfolio));
        when(quoteService.getCurrentQuote(10L)).thenReturn(QuoteDTO.builder().price(new BigDecimal("150")).build());
        when(orderRepository.findPendingSellOrdersForBuy(any(), any())).thenReturn(List.of());

        final Map<Long, Order> savedOrderMap = new HashMap<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) o.setId(99L + savedOrderMap.size());
            savedOrderMap.put(o.getId(), o);
            return o;
        });
        when(orderRepository.findById(anyLong())).thenAnswer(i ->
                Optional.ofNullable(savedOrderMap.get(i.getArgument(0)))
        );

        OrderDTO result = orderService.buy(1L, 10L, 10, "key", new BigDecimal("100"));

        assertEquals("PENDING", result.getStatus());
        assertEquals(10, result.getRemainingQuantity());
    }

    @Test
    void buy_autoSell_noSuperuserPortfolio() {
        User superuser = User.builder()
                .id(2L).username("superuser").email("super@test.com")
                .password("pass").role(User.Role.SUPERUSER).balance(BigDecimal.ZERO)
                .build();

        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(quoteService.getCurrentQuote(10L)).thenReturn(QuoteDTO.builder().price(new BigDecimal("100")).build());
        when(userRepository.findByUsername("superuser")).thenReturn(Optional.of(superuser));
        when(portfolioRepository.findByUserAndPlayer(superuser, player)).thenReturn(Optional.empty());
        when(orderRepository.findPendingSellOrdersForBuy(any(), any())).thenReturn(List.of());

        final Map<Long, Order> savedOrderMap = new HashMap<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) o.setId(99L + savedOrderMap.size());
            savedOrderMap.put(o.getId(), o);
            return o;
        });
        when(orderRepository.findById(anyLong())).thenAnswer(i ->
                Optional.ofNullable(savedOrderMap.get(i.getArgument(0)))
        );

        OrderDTO result = orderService.buy(1L, 10L, 10, "key", null);

        assertEquals("PENDING", result.getStatus());
        assertEquals(10, result.getRemainingQuantity());
        long autoSellCount = savedOrderMap.values().stream()
                .filter(o -> o.getType() == Order.OrderType.SELL
                        && o.getUser().getUsername().equals("superuser"))
                .count();
        assertEquals(0, autoSellCount);
    }

    @Test
    void matchSellOrder() {
        User buyer = User.builder().id(2L).username("buyer").email("buyer@test.com")
                .password("pass").role(User.Role.USER).balance(new BigDecimal("3000"))
                .build();
        Portfolio sellerPortfolio = Portfolio.builder()
                .user(user).player(player).tokenQty(10).avgBuyPrice(new BigDecimal("80"))
                .build();
        Order buyOrder = Order.builder()
                .id(300L).user(buyer).player(player).type(Order.OrderType.BUY)
                .quantity(5).priceAtOrder(new BigDecimal("100")).total(new BigDecimal("500"))
                .idempotencyKey("buy-match-key").status(Order.OrderStatus.PENDING).remainingQuantity(5)
                .build();

        when(orderRepository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(portfolioRepository.findByUserAndPlayer(user, player)).thenReturn(Optional.of(sellerPortfolio));
        when(orderRepository.findPendingBuyOrdersForSell(player, new BigDecimal("100")))
                .thenReturn(List.of(buyOrder));

        final Map<Long, Order> savedOrderMap = new HashMap<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            if (o.getId() == null) o.setId(99L + savedOrderMap.size());
            savedOrderMap.put(o.getId(), o);
            return o;
        });
        when(orderRepository.findById(anyLong())).thenAnswer(i ->
                Optional.ofNullable(savedOrderMap.get(i.getArgument(0)))
        );

        OrderDTO result = orderService.sell(1L, 10L, 8, "key", new BigDecimal("100"));

        assertEquals("PARTIALLY_FILLED", result.getStatus());
        assertEquals(3, result.getRemainingQuantity()); // 8 - 5 = 3
        verify(portfolioService).transferTokens(1L, 2L, 10L, 5, new BigDecimal("100"));
    }

    @Test
    void sellAll() {
        User seller = User.builder().id(1L).username("user").email("user@test.com")
                .password("pass").role(User.Role.USER).balance(new BigDecimal("5000"))
                .build();
        Player playerA = Player.builder().id(10L).name("Messi").availableTokens(100).totalTokens(100).build();
        Player playerB = Player.builder().id(11L).name("Ronaldo").availableTokens(100).totalTokens(100).build();
        Portfolio portA = Portfolio.builder().user(seller).player(playerA).tokenQty(5).avgBuyPrice(new BigDecimal("50")).build();
        Portfolio portB = Portfolio.builder().user(seller).player(playerB).tokenQty(3).avgBuyPrice(new BigDecimal("60")).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(portfolioRepository.findByUser(seller)).thenReturn(List.of(portA, portB));
        when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(playerRepository.findById(10L)).thenReturn(Optional.of(playerA));
        when(playerRepository.findById(11L)).thenReturn(Optional.of(playerB));
        when(portfolioRepository.findByUserAndPlayer(seller, playerA)).thenReturn(Optional.of(portA));
        when(portfolioRepository.findByUserAndPlayer(seller, playerB)).thenReturn(Optional.of(portB));
        when(quoteService.getCurrentQuote(anyLong())).thenReturn(QuoteDTO.builder().price(new BigDecimal("100")).build());
        when(orderRepository.findPendingBuyOrdersForSell(any(), any())).thenReturn(List.of());

        final List<Order> savedOrders = new ArrayList<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(99L + savedOrders.size());
            savedOrders.add(o);
            return o;
        });
        when(orderRepository.findById(anyLong())).thenAnswer(i -> {
            long id = i.getArgument(0);
            return savedOrders.stream().filter(o -> o.getId().equals(id)).findFirst().map(Optional::of).orElse(Optional.empty());
        });

        List<OrderDTO> results = orderService.sellAll(1L, "sell-all");

        assertEquals(2, results.size());
        assertEquals(5, results.get(0).getQuantity());
        assertEquals(3, results.get(1).getQuantity());
    }

    @Test
    void getOrdersByPlayer() {
        Player testPlayer = Player.builder().id(10L).name("Messi").build();
        Order orderA = Order.builder().id(1L).user(user).player(testPlayer).type(Order.OrderType.BUY)
                .quantity(5).priceAtOrder(new BigDecimal("100")).total(new BigDecimal("500"))
                .idempotencyKey("oba").status(Order.OrderStatus.PENDING).remainingQuantity(5)
                .build();
        Order orderB = Order.builder().id(2L).user(user).player(testPlayer).type(Order.OrderType.SELL)
                .quantity(3).priceAtOrder(new BigDecimal("110")).total(new BigDecimal("330"))
                .idempotencyKey("obb").status(Order.OrderStatus.PARTIALLY_FILLED).remainingQuantity(1)
                .build();

        when(playerRepository.findById(10L)).thenReturn(Optional.of(testPlayer));
        when(orderRepository.findByPlayerAndStatusIn(testPlayer,
                List.of(Order.OrderStatus.PENDING, Order.OrderStatus.PARTIALLY_FILLED)))
                .thenReturn(List.of(orderA, orderB));

        List<OrderDTO> result = orderService.getOrdersByPlayer(10L);

        assertEquals(2, result.size());
        assertEquals("PENDING", result.get(0).getStatus());
        assertEquals("PARTIALLY_FILLED", result.get(1).getStatus());
    }
}
