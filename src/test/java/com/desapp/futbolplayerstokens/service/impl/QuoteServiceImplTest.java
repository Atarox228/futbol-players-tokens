package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Quote;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.service.ValuationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteServiceImplTest {

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private ValuationService valuationService;

    @Mock
    private StrategyConfigRepository strategyConfigRepository;

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private QuoteServiceImpl quoteService;

    private Player testPlayer;
    private StrategyConfig testStrategyConfig;
    private Quote testQuote;
    private ValuationResult testValuationResult;

    @BeforeEach
    void setUp() {
        testPlayer = Player.builder()
                .id(1L)
                .name("Messi")
                .team("Barcelona")
                .league("LaLiga")
                .position("RW")
                .rating(9.5)
                .build();

        testStrategyConfig = StrategyConfig.builder()
                .id(1L)
                .version(1)
                .build();

        testQuote = Quote.builder()
                .id(10L)
                .player(testPlayer)
                .price(new BigDecimal("10000.50"))
                .timestamp(LocalDateTime.of(2026, 5, 5, 12, 0, 0))
                .strategyId(1L)
                .strategyVersion(1)
                .trigger(QuoteTrigger.MANUAL)
                .build();

        testValuationResult = ValuationResult.builder()
                .price(new BigDecimal("12000.75"))
                .strategyId(1L)
                .strategyVersion(1)
                .build();
    }

    @Test
    void testGetQuotesByPlayerId_WhenQuotesExist_ShouldReturnList() {
        // Arrange
        Long playerId = 1L;
        List<Quote> existingQuotes = List.of(testQuote);
        when(quoteRepository.findByPlayerIdOrderByTimestampDesc(playerId))
                .thenReturn(existingQuotes);

        // Act
        List<QuoteDTO> result = quoteService.getQuotesByPlayerId(playerId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(playerId, result.get(0).getPlayerId());
        assertEquals(new BigDecimal("10000.50"), result.get(0).getPrice());
        assertEquals("MANUAL", result.get(0).getTrigger());

        verify(quoteRepository, times(1)).findByPlayerIdOrderByTimestampDesc(playerId);
        verify(valuationService, never()).evaluatePlayer(anyLong(), anyLong());
        verify(strategyConfigRepository, never()).findAll();
    }

    @Test
    void testGetQuotesByPlayerId_WhenNoQuotesExist_ShouldCreateAndReturnNewQuote() {
        // Arrange
        Long playerId = 1L;
        when(quoteRepository.findByPlayerIdOrderByTimestampDesc(playerId))
                .thenReturn(List.of());
        when(playerRepository.findById(playerId))
                .thenReturn(Optional.of(testPlayer));
        when(strategyConfigRepository.findAll())
                .thenReturn(List.of(testStrategyConfig));
        when(valuationService.evaluatePlayer(playerId, testStrategyConfig.getId()))
                .thenReturn(testValuationResult);
        when(quoteRepository.save(any(Quote.class)))
                .thenAnswer(invocation -> {
                    Quote q = invocation.getArgument(0);
                    q.setId(20L);
                    return q;
                });

        // Act
        List<QuoteDTO> result = quoteService.getQuotesByPlayerId(playerId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).getId());
        assertEquals(playerId, result.get(0).getPlayerId());
        assertEquals(new BigDecimal("12000.75"), result.get(0).getPrice());
        assertEquals("MANUAL", result.get(0).getTrigger());

        verify(quoteRepository, times(1)).findByPlayerIdOrderByTimestampDesc(playerId);
        verify(playerRepository, times(1)).findById(playerId);
        verify(strategyConfigRepository, times(1)).findAll();
        verify(valuationService, times(1)).evaluatePlayer(playerId, testStrategyConfig.getId());
        verify(quoteRepository, times(1)).save(any(Quote.class));
    }

    @Test
    void testGetQuotesByPlayerId_WhenPlayerNotFound_ShouldThrowException() {
        // Arrange
        Long playerId = 999L;
        when(quoteRepository.findByPlayerIdOrderByTimestampDesc(playerId))
                .thenReturn(List.of());
        when(playerRepository.findById(playerId))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            quoteService.getQuotesByPlayerId(playerId);
        });
        assertEquals("Player not found with id: 999", exception.getMessage());

        verify(quoteRepository, times(1)).findByPlayerIdOrderByTimestampDesc(playerId);
        verify(playerRepository, times(1)).findById(playerId);
        verify(strategyConfigRepository, never()).findAll();
        verify(valuationService, never()).evaluatePlayer(anyLong(), anyLong());
    }

    @Test
    void testGetQuotesByPlayerId_WhenNoStrategyConfigAvailable_ShouldThrowException() {
        // Arrange
        Long playerId = 1L;
        when(quoteRepository.findByPlayerIdOrderByTimestampDesc(playerId))
                .thenReturn(List.of());
        when(playerRepository.findById(playerId))
                .thenReturn(Optional.of(testPlayer));
        when(strategyConfigRepository.findAll())
                .thenReturn(List.of());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            quoteService.getQuotesByPlayerId(playerId);
        });
        assertEquals("No strategy config available to calculate quote", exception.getMessage());

        verify(quoteRepository, times(1)).findByPlayerIdOrderByTimestampDesc(playerId);
        verify(playerRepository, times(1)).findById(playerId);
        verify(strategyConfigRepository, times(1)).findAll();
        verify(valuationService, never()).evaluatePlayer(anyLong(), anyLong());
    }

    @Test
    void testGetQuotesByPlayerId_WhenMultipleQuotesExist_ShouldReturnAllOrdered() {
        // Arrange
        Long playerId = 1L;
        Quote quote2 = Quote.builder()
                .id(11L)
                .player(testPlayer)
                .price(new BigDecimal("11000.00"))
                .timestamp(LocalDateTime.of(2026, 5, 4, 12, 0, 0))
                .strategyId(1L)
                .strategyVersion(1)
                .trigger(QuoteTrigger.SCHEDULED)
                .build();

        List<Quote> existingQuotes = List.of(testQuote, quote2); // already ordered by timestamp DESC
        when(quoteRepository.findByPlayerIdOrderByTimestampDesc(playerId))
                .thenReturn(existingQuotes);

        // Act
        List<QuoteDTO> result = quoteService.getQuotesByPlayerId(playerId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(11L, result.get(1).getId());

        verify(quoteRepository, times(1)).findByPlayerIdOrderByTimestampDesc(playerId);
        verify(valuationService, never()).evaluatePlayer(anyLong(), anyLong());
    }

    @Test
    void testGetQuotesByPlayerId_WhenMultipleStrategyConfigsExist_ShouldUseLatestVersion() {
        // Arrange
        Long playerId = 1L;
        StrategyConfig configV1 = StrategyConfig.builder().id(1L).version(1).build();
        StrategyConfig configV2 = StrategyConfig.builder().id(2L).version(2).build();
        StrategyConfig configV3 = StrategyConfig.builder().id(3L).version(3).build();

        when(quoteRepository.findByPlayerIdOrderByTimestampDesc(playerId))
                .thenReturn(List.of());
        when(playerRepository.findById(playerId))
                .thenReturn(Optional.of(testPlayer));
        when(strategyConfigRepository.findAll())
                .thenReturn(List.of(configV1, configV2, configV3));
        when(valuationService.evaluatePlayer(playerId, configV3.getId()))
                .thenReturn(testValuationResult);
        when(quoteRepository.save(any(Quote.class)))
                .thenAnswer(invocation -> {
                    Quote q = invocation.getArgument(0);
                    q.setId(20L);
                    return q;
                });

        // Act
        List<QuoteDTO> result = quoteService.getQuotesByPlayerId(playerId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(valuationService, times(1)).evaluatePlayer(playerId, 3L); // should use configV3 (version 3)
    }
}

