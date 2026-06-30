package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.exception.ConfigurationException;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Quote;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig.StrategyType;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.modelo.ValuationMode;
import com.desapp.futbolplayerstokens.service.ScoringConfigService;
import com.desapp.futbolplayerstokens.service.ValuationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private ScoringConfigService scoringConfigService;

    private QuoteServiceImpl quoteService;

    @Captor
    private ArgumentCaptor<Quote> quoteCaptor;

    private Player testPlayer;
    private StrategyConfig testStrategyConfig;
    private Quote testQuote;
    private ValuationResult testValuationResult;

    @BeforeEach
    void setUp() {
        quoteService = new QuoteServiceImpl(quoteRepository, playerRepository, strategyConfigRepository,
                valuationService, transactionTemplate, userRepository, portfolioRepository,
                scoringConfigService, new SimpleMeterRegistry());
        testPlayer = Player.builder()
                .id(1L)
                .name("Messi")
                .team("Barcelona")
                .league("LaLiga")
                .rating(9.5)
                .build();

        testStrategyConfig = StrategyConfig.builder()
                .id(1L)
                .version(1)
                .type(StrategyType.GENERAL)
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

        lenient().doAnswer(invocation -> {
            Consumer<?> consumer = invocation.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        lenient().when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        lenient().when(scoringConfigService.getActiveMode()).thenReturn(ValuationMode.GENERAL);
    }

    @Test
    void getQuotesByPlayerId_WhenQuotesExist_returnsDtosWithoutCallingValuation() {
        Long playerId = 1L;
        when(quoteRepository.findByPlayerIdOrderByTimestampDesc(playerId)).thenReturn(List.of(testQuote));

        List<QuoteDTO> result = quoteService.getQuotesByPlayerId(playerId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.getFirst().getId());
        verify(valuationService, never()).evaluatePlayer(anyLong(), anyLong(), any());
    }

    @Test
    void getQuotesByPlayerId_WhenNoQuotes_exist_createsQuoteViaRecalculateSingle() {
        Long playerId = 1L;
        when(quoteRepository.findByPlayerIdOrderByTimestampDesc(playerId)).thenReturn(List.of());
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(testPlayer));
        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.GENERAL)).thenReturn(Optional.of(testStrategyConfig));
        when(valuationService.evaluatePlayer(eq(playerId), eq(testStrategyConfig.getId()), isNull()))
                .thenReturn(testValuationResult);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> {
            Quote q = invocation.getArgument(0);
            q.setId(20L);
            return q;
        });

        List<QuoteDTO> result = quoteService.getQuotesByPlayerId(playerId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(20L, result.getFirst().getId());
        assertEquals("MANUAL", result.getFirst().getTrigger());
        verify(valuationService, times(1)).evaluatePlayer(eq(playerId), eq(testStrategyConfig.getId()), isNull());
    }

    @Test
    void getCurrentQuote_WhenExists_returnsTopQuoteDto() {
        Long playerId = 1L;
        when(quoteRepository.findTopByPlayerIdOrderByTimestampDesc(playerId)).thenReturn(Optional.of(testQuote));

        QuoteDTO dto = quoteService.getCurrentQuote(playerId);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        verify(valuationService, never()).evaluatePlayer(anyLong(), anyLong(), any());
    }

    @Test
    void getCurrentQuote_WhenMissing_createsViaRecalculateSingle() {
        Long playerId = 1L;
        when(quoteRepository.findTopByPlayerIdOrderByTimestampDesc(playerId)).thenReturn(Optional.empty());
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(testPlayer));
        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.GENERAL)).thenReturn(Optional.of(testStrategyConfig));
        when(valuationService.evaluatePlayer(eq(playerId), eq(testStrategyConfig.getId()), isNull()))
                .thenReturn(testValuationResult);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuoteDTO dto = quoteService.getCurrentQuote(playerId);

        assertNotNull(dto);
        assertEquals(new BigDecimal("12000.75"), dto.getPrice());
        verify(valuationService, times(1)).evaluatePlayer(eq(playerId), eq(testStrategyConfig.getId()), isNull());
    }

    @Test
    void recalculateAll_withThreePlayers_callsValuationThreeTimes_andSavesQuotesWithScheduledTrigger() {
        Player p1 = Player.builder().id(1L).build();
        Player p2 = Player.builder().id(2L).build();
        Player p3 = Player.builder().id(3L).build();
        List<Player> players = List.of(p1, p2, p3);

        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.GENERAL)).thenReturn(Optional.of(testStrategyConfig));
        when(playerRepository.findAll()).thenReturn(players);
        when(valuationService.evaluatePlayer(anyLong(), eq(testStrategyConfig.getId()), isNull()))
                .thenReturn(testValuationResult);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        quoteService.recalculateAll(QuoteTrigger.SCHEDULED);

        verify(valuationService, times(3)).evaluatePlayer(anyLong(), eq(testStrategyConfig.getId()), isNull());
        verify(quoteRepository, times(3)).save(quoteCaptor.capture());

        List<Quote> saved = quoteCaptor.getAllValues();
        assertEquals(3, saved.size());
        for (Quote q : saved) {
            assertEquals(QuoteTrigger.SCHEDULED, q.getTrigger());
        }
    }

    @Test
    void recalculateAll_whenManual_savesQuotesWithManualTrigger() {
        Player p1 = Player.builder().id(1L).build();
        Player p2 = Player.builder().id(2L).build();

        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.GENERAL)).thenReturn(Optional.of(testStrategyConfig));
        when(playerRepository.findAll()).thenReturn(List.of(p1, p2));
        when(valuationService.evaluatePlayer(anyLong(), eq(testStrategyConfig.getId()), isNull()))
                .thenReturn(testValuationResult);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        quoteService.recalculateAll(QuoteTrigger.MANUAL);

        verify(valuationService, times(2)).evaluatePlayer(anyLong(), eq(testStrategyConfig.getId()), isNull());
        verify(quoteRepository, times(2)).save(quoteCaptor.capture());
        assertTrue(quoteCaptor.getAllValues().stream().allMatch(q -> q.getTrigger() == QuoteTrigger.MANUAL));
    }

    @Test
    void recalculateAll_whenNoStrategyConfig_throwsConfigurationException() {
        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.GENERAL)).thenReturn(Optional.empty());

        assertThrows(ConfigurationException.class, () -> quoteService.recalculateAll(QuoteTrigger.SCHEDULED));
        verify(playerRepository, never()).findAll();
        verify(valuationService, never()).evaluatePlayer(anyLong(), anyLong(), any());
    }

    @Test
    void recalculateAll_withPositionMode_usesPositionTypeConfig() {
        Player fw = Player.builder().id(1L).position("FW").build();
        Player gk = Player.builder().id(2L).position("GK").build();

        StrategyConfig forwardCfg = StrategyConfig.builder()
                .id(2L).version(1).type(StrategyType.FORWARD).build();

        when(scoringConfigService.getActiveMode()).thenReturn(ValuationMode.POSITION);
        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.GENERAL))
                .thenReturn(Optional.of(testStrategyConfig));
        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.FORWARD))
                .thenReturn(Optional.of(forwardCfg));
        when(playerRepository.findAll()).thenReturn(List.of(fw, gk));
        when(valuationService.evaluatePlayer(eq(1L), eq(2L), eq("POSITION")))
                .thenReturn(testValuationResult);
        when(valuationService.evaluatePlayer(eq(2L), eq(testStrategyConfig.getId()), eq("POSITION")))
                .thenReturn(testValuationResult);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        quoteService.recalculateAll(QuoteTrigger.MANUAL);

        verify(valuationService).evaluatePlayer(eq(1L), eq(2L), eq("POSITION"));
        verify(valuationService).evaluatePlayer(eq(2L), eq(testStrategyConfig.getId()), eq("POSITION"));
        verify(quoteRepository, times(2)).save(any(Quote.class));
    }

    @Test
    void recalculatePlayers_withSpecificIds_onlyProcessesGivenPlayers() {
        Player p1 = Player.builder().id(1L).build();
        Player p2 = Player.builder().id(2L).build();

        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.GENERAL))
                .thenReturn(Optional.of(testStrategyConfig));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(p2));
        when(valuationService.evaluatePlayer(anyLong(), eq(testStrategyConfig.getId()), isNull()))
                .thenReturn(testValuationResult);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        quoteService.recalculatePlayers(List.of(1L, 2L), QuoteTrigger.MANUAL);

        verify(valuationService, times(2)).evaluatePlayer(anyLong(), eq(testStrategyConfig.getId()), isNull());
        verify(quoteRepository, times(2)).save(any(Quote.class));
        verify(playerRepository, never()).findAll();
    }

    @Test
    void recalculatePlayers_withEmptyList_doesNothing() {
        quoteService.recalculatePlayers(List.of(), QuoteTrigger.MANUAL);

        verify(strategyConfigRepository, never()).findTopByTypeOrderByVersionDesc(any());
        verify(playerRepository, never()).findAll();
        verify(valuationService, never()).evaluatePlayer(anyLong(), anyLong(), any());
    }

    @Test
    void getCurrentQuote_withPositionMode_resolvesPositionConfig() {
        Player fw = Player.builder().id(1L).position("FW").build();
        StrategyConfig forwardCfg = StrategyConfig.builder()
                .id(2L).version(1).type(StrategyType.FORWARD).build();

        when(quoteRepository.findTopByPlayerIdOrderByTimestampDesc(1L)).thenReturn(Optional.empty());
        when(playerRepository.findById(1L)).thenReturn(Optional.of(fw));
        when(scoringConfigService.getActiveMode()).thenReturn(ValuationMode.POSITION);
        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.GENERAL))
                .thenReturn(Optional.of(testStrategyConfig));
        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.FORWARD))
                .thenReturn(Optional.of(forwardCfg));
        when(valuationService.evaluatePlayer(eq(1L), eq(2L), eq("POSITION")))
                .thenReturn(testValuationResult);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuoteDTO dto = quoteService.getCurrentQuote(1L);

        assertNotNull(dto);
        verify(valuationService).evaluatePlayer(eq(1L), eq(2L), eq("POSITION"));
    }

    @Test
    void getQuotesByPlayerId_withPositionMode_resolvesPositionConfig() {
        Player fw = Player.builder().id(1L).position("DELANTERO").build();
        StrategyConfig forwardCfg = StrategyConfig.builder()
                .id(3L).version(1).type(StrategyType.FORWARD).build();

        when(quoteRepository.findByPlayerIdOrderByTimestampDesc(1L)).thenReturn(List.of());
        when(playerRepository.findById(1L)).thenReturn(Optional.of(fw));
        when(scoringConfigService.getActiveMode()).thenReturn(ValuationMode.POSITION);
        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.GENERAL))
                .thenReturn(Optional.of(testStrategyConfig));
        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.FORWARD))
                .thenReturn(Optional.of(forwardCfg));
        when(valuationService.evaluatePlayer(eq(1L), eq(3L), eq("POSITION")))
                .thenReturn(testValuationResult);
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<QuoteDTO> result = quoteService.getQuotesByPlayerId(1L);

        assertEquals(1, result.size());
        verify(valuationService).evaluatePlayer(eq(1L), eq(3L), eq("POSITION"));
    }
}

