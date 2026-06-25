package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.MarketDepthDTO;
import com.desapp.futbolplayerstokens.controller.dto.MarketOverviewDTO;
import com.desapp.futbolplayerstokens.controller.dto.OrderBookStatsDTO;
import com.desapp.futbolplayerstokens.controller.dto.PlayerValuationDTO;
import com.desapp.futbolplayerstokens.controller.dto.PortfolioSummaryDTO;
import com.desapp.futbolplayerstokens.controller.dto.StrategyImpactDTO;
import com.desapp.futbolplayerstokens.controller.dto.TopTradedDTO;
import com.desapp.futbolplayerstokens.exception.ResourceNotFoundException;
import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Order.OrderStatus;
import com.desapp.futbolplayerstokens.modelo.Order.OrderType;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.Quote;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.OrderRepository;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsControllerRESTTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private PortfolioRepository portfolioRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private UserRepository userRepository;
    @Mock private StrategyConfigRepository strategyConfigRepository;

    @InjectMocks
    private MetricsControllerREST controller;

    private Player testPlayer;
    private User testUser;
    private Order buyOrder;
    private Order sellOrder;
    private Portfolio portfolio;
    private Quote quote;

    @BeforeEach
    void setUp() {
        testPlayer = Player.builder()
                .id(1L).name("Messi").team("Barcelona").league("LaLiga")
                .position("FW").score(new BigDecimal("85"))
                .totalTokens(100).availableTokens(50)
                .build();

        testUser = User.builder()
                .id(1L).username("testuser").balance(new BigDecimal("5000"))
                .build();

        buyOrder = Order.builder()
                .id(10L).user(testUser).player(testPlayer)
                .type(OrderType.BUY).quantity(5).priceAtOrder(new BigDecimal("100"))
                .total(new BigDecimal("500")).status(OrderStatus.PENDING).remainingQuantity(5)
                .build();

        sellOrder = Order.builder()
                .id(11L).user(testUser).player(testPlayer)
                .type(OrderType.SELL).quantity(3).priceAtOrder(new BigDecimal("110"))
                .total(new BigDecimal("330")).status(OrderStatus.PENDING).remainingQuantity(3)
                .build();

        portfolio = Portfolio.builder()
                .user(testUser).player(testPlayer)
                .tokenQty(10).avgBuyPrice(new BigDecimal("80"))
                .build();

        quote = Quote.builder()
                .id(100L).player(testPlayer).price(new BigDecimal("95"))
                .timestamp(LocalDateTime.now()).strategyId(1L).strategyVersion(2)
                .build();
    }

    @Test
    void marketOverview_returnsAllStats() {
        when(orderRepository.countByStatusAndType(OrderStatus.PENDING, OrderType.BUY)).thenReturn(5L);
        when(orderRepository.countByStatusAndType(OrderStatus.PENDING, OrderType.SELL)).thenReturn(3L);
        when(orderRepository.sumTotalByStatusAndType(OrderStatus.PENDING, OrderType.BUY)).thenReturn(new BigDecimal("2500"));
        when(orderRepository.sumTotalByStatusAndType(OrderStatus.PENDING, OrderType.SELL)).thenReturn(new BigDecimal("1200"));
        when(portfolioRepository.countDistinctUserId()).thenReturn(10L);
        when(playerRepository.count()).thenReturn(100L);
        when(playerRepository.findAll()).thenReturn(List.of(testPlayer));

        MarketOverviewDTO result = controller.marketOverview().getBody();

        assertNotNull(result);
        assertEquals(5, result.getOpenBuyOrders());
        assertEquals(3, result.getOpenSellOrders());
        assertEquals(new BigDecimal("2500"), result.getTotalValueLockedBuy());
        assertEquals(new BigDecimal("1200"), result.getTotalValueLockedSell());
        assertEquals(10, result.getActiveUsers());
        assertEquals(100, result.getTotalPlayers());
        assertEquals(100, result.getTotalTokensInCirculation());
    }

    @Test
    void marketDepth_returnsBidAskLevels() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
        when(orderRepository.findByPlayerAndStatusIn(eq(testPlayer), anyList()))
                .thenReturn(List.of(buyOrder, sellOrder));

        MarketDepthDTO result = controller.marketDepth(1L).getBody();

        assertNotNull(result);
        assertEquals(1L, result.getPlayerId());
        assertEquals("Messi", result.getPlayerName());
        assertEquals(new BigDecimal("100"), result.getBestBid());
        assertEquals(new BigDecimal("110"), result.getBestAsk());
        assertEquals(new BigDecimal("10"), result.getSpread());
        assertEquals(1, result.getBids().size());
        assertEquals(1, result.getAsks().size());
    }

    @Test
    void marketDepth_playerNotFound_throws() {
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> controller.marketDepth(99L));
    }

    @Test
    void marketDepth_emptyMarket_returnsZeroSpread() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
        when(orderRepository.findByPlayerAndStatusIn(eq(testPlayer), anyList()))
                .thenReturn(List.of());

        MarketDepthDTO result = controller.marketDepth(1L).getBody();

        assertNotNull(result);
        assertTrue(result.getBids().isEmpty());
        assertTrue(result.getAsks().isEmpty());
        assertEquals(BigDecimal.ZERO, result.getBestBid());
        assertEquals(BigDecimal.ZERO, result.getBestAsk());
        assertEquals(BigDecimal.ZERO, result.getSpread());
    }

    @Test
    void playerValuation_returnsPriceAndChanges() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
        when(quoteRepository.findTopByPlayerIdOrderByTimestampDesc(1L))
                .thenReturn(Optional.of(quote));
        when(quoteRepository.findPricesByPlayerIdSince(eq(1L), any()))
                .thenReturn(List.of(new BigDecimal("95"), new BigDecimal("90"), new BigDecimal("85")));

        PlayerValuationDTO result = controller.playerValuation(1L).getBody();

        assertNotNull(result);
        assertEquals(1L, result.getPlayerId());
        assertEquals("Messi", result.getPlayerName());
        assertEquals(new BigDecimal("95"), result.getCurrentPrice());
        assertEquals(new BigDecimal("85"), result.getScore());
        assertEquals("FW", result.getPosition());
        assertEquals("Barcelona", result.getTeam());
    }

    @Test
    void topTraded_returnsRankedPlayers() {
        when(orderRepository.findTopTradedPlayers()).thenReturn(Collections.singletonList(
                new Object[]{1L, 10L, 50L, new BigDecimal("5000")}
        ));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(testPlayer));

        List<TopTradedDTO> result = controller.topTraded().getBody();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().getRank());
        assertEquals("Messi", result.getFirst().getPlayerName());
        assertEquals(10, result.getFirst().getOrderCount());
        assertEquals(50, result.getFirst().getTotalQuantity());
    }

    @Test
    void portfolioSummary_returnsFullSummary() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(portfolioRepository.findByUser(testUser)).thenReturn(List.of(portfolio));
        when(quoteRepository.findTopByPlayerIdOrderByTimestampDesc(1L))
                .thenReturn(Optional.of(quote));

        PortfolioSummaryDTO result = controller.portfolioSummary(1L).getBody();

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("testuser", result.getUsername());
        assertEquals(new BigDecimal("800"), result.getTotalInvested());
        assertEquals(new BigDecimal("950"), result.getCurrentValue());
        assertEquals(new BigDecimal("150"), result.getProfitLoss());
        assertEquals(1, result.getTotalPositions());
        assertEquals(1, result.getDiversification().getForwardCount());
        assertEquals(1, result.getDiversification().getLaLigaCount());
    }

    @Test
    void portfolioSummary_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> controller.portfolioSummary(99L));
    }

    @Test
    void orderBookStats_returnsStats() {
        when(orderRepository.count()).thenReturn(100L);
        when(orderRepository.findByStatus(OrderStatus.FILLED)).thenReturn(List.of(buyOrder));
        when(orderRepository.findByStatus(OrderStatus.CANCELLED)).thenReturn(List.of());
        when(orderRepository.findByStatus(OrderStatus.PENDING)).thenReturn(List.of(sellOrder));

        OrderBookStatsDTO result = controller.orderBookStats().getBody();

        assertNotNull(result);
        assertEquals(100, result.getTotalOrders());
        assertEquals(1, result.getFilledOrders());
        assertEquals(0, result.getCancelledOrders());
        assertEquals(1, result.getPendingOrders());
        assertEquals(0.01, result.getFillRate(), 0.001);
    }

    @Test
    void strategyImpact_returnsImpacts() {
        StrategyConfig oldCfg = StrategyConfig.builder()
                .id(1L).version(1).type(StrategyConfig.StrategyType.GENERAL).build();
        StrategyConfig newCfg = StrategyConfig.builder()
                .id(2L).version(2).type(StrategyConfig.StrategyType.GENERAL).build();

        when(strategyConfigRepository.findAll()).thenReturn(List.of(oldCfg, newCfg));
        when(strategyConfigRepository.findByTypeOrderByVersionDesc(StrategyConfig.StrategyType.GENERAL))
                .thenReturn(List.of(newCfg, oldCfg));
        lenient().when(strategyConfigRepository.findFirstByTypeAndVersionLessThanOrderByVersionDesc(
                        StrategyConfig.StrategyType.GENERAL, 2))
                .thenReturn(Optional.of(oldCfg));
        lenient().when(quoteRepository.findByStrategyAndVersionAndTimestampBetween(
                        eq(2L), eq(2), any(), any()))
                .thenReturn(List.of());
        lenient().when(quoteRepository.findByStrategyAndVersionAndTimestampBetween(
                        eq(1L), eq(1), any(), any()))
                .thenReturn(List.of());

        List<StrategyImpactDTO> result = controller.strategyImpact().getBody();

        assertNotNull(result);
    }

    @Test
    void allEndpointsReturnOkStatus() {
        when(orderRepository.countByStatusAndType(any(), any())).thenReturn(0L);
        when(orderRepository.sumTotalByStatusAndType(any(), any())).thenReturn(BigDecimal.ZERO);
        when(portfolioRepository.countDistinctUserId()).thenReturn(0L);
        when(playerRepository.count()).thenReturn(0L);
        when(playerRepository.findAll()).thenReturn(List.of());
        when(orderRepository.findTopTradedPlayers()).thenReturn(List.of());
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.findByStatus(any())).thenReturn(List.of());
        when(strategyConfigRepository.findAll()).thenReturn(List.of());

        assertNotNull(controller.marketOverview());
        assertNotNull(controller.topTraded());
        assertNotNull(controller.orderBookStats());
        assertNotNull(controller.strategyImpact());
    }
}
