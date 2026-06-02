package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PortfolioDTO;
import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.service.QuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private QuoteService quoteService;

    private PortfolioServiceImpl portfolioService;
    private User user;
    private Player player;

    @BeforeEach
    void setUp() {
        portfolioService = new PortfolioServiceImpl(portfolioRepository, playerRepository, userRepository,
                quoteRepository, quoteService);

        user = User.builder()
                .id(1L)
                .username("user")
                .email("user@example.com")
                .password("password")
                .role(User.Role.USER)
                .balance(new BigDecimal("1000"))
                .build();
        player = Player.builder()
                .id(10L)
                .name("Messi")
                .build();
    }

    @Test
    void updatePosition_buyNewPortfolio() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(portfolioRepository.findByUserAndPlayer(user, player)).thenReturn(Optional.empty());

        portfolioService.updatePosition(1L, 10L, 2, new BigDecimal("150"), Order.OrderType.BUY);

        ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
        verify(portfolioRepository).save(captor.capture());
        assertEquals(user, captor.getValue().getUser());
        assertEquals(player, captor.getValue().getPlayer());
        assertEquals(2, captor.getValue().getTokenQty());
        assertEquals(new BigDecimal("150"), captor.getValue().getAvgBuyPrice());
    }

    @Test
    void updatePosition_buyExistingPortfolio() {
        Portfolio existing = Portfolio.builder()
                .user(user)
                .player(player)
                .tokenQty(3)
                .avgBuyPrice(new BigDecimal("100"))
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(portfolioRepository.findByUserAndPlayer(user, player)).thenReturn(Optional.of(existing));

        portfolioService.updatePosition(1L, 10L, 2, new BigDecimal("150"), Order.OrderType.BUY);

        assertEquals(5, existing.getTokenQty());
        assertEquals(new BigDecimal("120.00000000"), existing.getAvgBuyPrice());
        verify(portfolioRepository).save(existing);
    }

    @Test
    void updatePosition_sellPartial() {
        Portfolio existing = Portfolio.builder()
                .user(user)
                .player(player)
                .tokenQty(5)
                .avgBuyPrice(new BigDecimal("100"))
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(portfolioRepository.findByUserAndPlayer(user, player)).thenReturn(Optional.of(existing));

        portfolioService.updatePosition(1L, 10L, 2, new BigDecimal("150"), Order.OrderType.SELL);

        assertEquals(3, existing.getTokenQty());
        assertEquals(new BigDecimal("100"), existing.getAvgBuyPrice());
        verify(portfolioRepository).save(existing);
        verify(portfolioRepository, never()).delete(any());
    }

    @Test
    void updatePosition_sellAll() {
        Portfolio existing = Portfolio.builder()
                .user(user)
                .player(player)
                .tokenQty(3)
                .avgBuyPrice(new BigDecimal("100"))
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(portfolioRepository.findByUserAndPlayer(user, player)).thenReturn(Optional.of(existing));

        portfolioService.updatePosition(1L, 10L, 3, new BigDecimal("150"), Order.OrderType.SELL);

        verify(portfolioRepository).delete(existing);
        verify(portfolioRepository, never()).save(existing);
    }

    @Test
    void getPortfolio_withPositions() {
        Portfolio existing = Portfolio.builder()
                .user(user)
                .player(player)
                .tokenQty(2)
                .avgBuyPrice(new BigDecimal("100"))
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(portfolioRepository.findByUser(user)).thenReturn(List.of(existing));
        when(quoteService.getCurrentQuote(10L)).thenReturn(QuoteDTO.builder().price(new BigDecimal("120")).build());

        List<PortfolioDTO> result = portfolioService.getPortfolio(1L);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("240.00000000"), result.getFirst().getCurrentValue());
        assertEquals(new BigDecimal("40.00000000"), result.getFirst().getProfitLoss());
        assertEquals(new BigDecimal("120"), result.getFirst().getCurrentPrice());
    }
}
