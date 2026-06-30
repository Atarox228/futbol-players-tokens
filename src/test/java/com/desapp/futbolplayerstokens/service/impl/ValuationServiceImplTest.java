 package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.ValuationContext;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.service.RankingService;
import com.desapp.futbolplayerstokens.service.Strategy;
import com.desapp.futbolplayerstokens.service.ValuationStrategyRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValuationServiceImplTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private StrategyConfigRepository strategyConfigRepository;

    @Mock
    private ValuationStrategyRouter valuationStrategyRouter;

        @Mock
        private RankingService rankingService;

    @Mock
    private Strategy strategy;

    @InjectMocks
    private ValuationServiceImpl valuationService;

    @Test
    void shouldEvaluatePlayerWithGeneralStrategyAndPersistOnlyScore() {
        Player player = Player.builder().id(10L).position("UNKNOWN").build();
        StrategyConfig strategyConfig = StrategyConfig.builder()
                .id(20L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("50.00"))
                .version(4)
                .build();
        ValuationResult expectedResult = ValuationResult.builder()
                .price(new BigDecimal("123.45000000"))
                .strategyId(20L)
                .strategyVersion(4)
                .build();

        when(playerRepository.findById(10L)).thenReturn(Optional.of(player));
        when(strategyConfigRepository.findById(20L)).thenReturn(Optional.of(strategyConfig));
        when(valuationStrategyRouter.resolve(null)).thenReturn(strategy);
        when(strategy.evaluate(any(ValuationContext.class))).thenReturn(expectedResult);
        when(playerRepository.updateScoreById(10L, expectedResult.getPrice())).thenReturn(1);

        ValuationResult result = valuationService.evaluatePlayer(10L, 20L);

        assertEquals(expectedResult, result);
        ArgumentCaptor<ValuationContext> contextCaptor = ArgumentCaptor.forClass(ValuationContext.class);
        verify(valuationStrategyRouter).resolve(null);
        verify(strategy).evaluate(contextCaptor.capture());
        assertSame(player, contextCaptor.getValue().getPlayer());
        assertSame(strategyConfig, contextCaptor.getValue().getStrategyConfig());
        verify(playerRepository).updateScoreById(10L, expectedResult.getPrice());
                verify(rankingService).invalidateCache();
    }

    @Test
    void shouldEvaluatePlayerWithExplicitStrategyKey() {
        Player player = Player.builder().id(11L).position("FW").build();
        StrategyConfig strategyConfig = StrategyConfig.builder()
                .id(21L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("50.00"))
                .version(5)
                .build();
        ValuationResult expectedResult = ValuationResult.builder()
                .price(new BigDecimal("222.00000000"))
                .strategyId(21L)
                .strategyVersion(5)
                .build();

        when(playerRepository.findById(11L)).thenReturn(Optional.of(player));
        when(strategyConfigRepository.findById(21L)).thenReturn(Optional.of(strategyConfig));
        when(valuationStrategyRouter.resolve("POSITION")).thenReturn(strategy);
        when(strategy.evaluate(any(ValuationContext.class))).thenReturn(expectedResult);
        when(playerRepository.updateScoreById(11L, expectedResult.getPrice())).thenReturn(1);

        ValuationResult result = valuationService.evaluatePlayer(11L, 21L, "POSITION");

        assertEquals(expectedResult, result);
        verify(valuationStrategyRouter).resolve("POSITION");
        verify(strategy).evaluate(any(ValuationContext.class));
        verify(playerRepository).updateScoreById(11L, expectedResult.getPrice());
                verify(rankingService).invalidateCache();
    }
}

