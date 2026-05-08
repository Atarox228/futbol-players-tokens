package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.ValuationContext;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreByPositionStrategyTest {

    private final ScoreByPositionStrategy strategy = new ScoreByPositionStrategy();

    @Test
    void shouldCalculateForwardPrice() {
        Map<String, BigDecimal> weights = new HashMap<>();
        Player player = Player.builder()
                .id(1L)
                .position("FW")
                .goals(10)
                .shotsOnTarget(5.0)
                .build();

        StrategyConfig strategyConfig = StrategyConfig.builder()
                .id(50L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("10.00"))
                .version(1)
                .weights(weights)
                .build();

        ValuationResult result = strategy.evaluate(ValuationContext.builder()
                .player(player)
                .strategyConfig(strategyConfig)
                .build());

        assertEquals(new BigDecimal("180.00000000"), result.getPrice());
        assertEquals(50L, result.getStrategyId());
        assertEquals(1, result.getStrategyVersion());
    }

    @Test
    void shouldCalculateGoalkeeperPrice() {
        Player player = Player.builder()
                .id(2L)
                .position("GK")
                .clears(4.0)
                .build();

        StrategyConfig strategyConfig = StrategyConfig.builder()
                .id(51L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("10.00"))
                .version(1)
                .build();

        ValuationResult result = strategy.evaluate(ValuationContext.builder()
                .player(player)
                .strategyConfig(strategyConfig)
                .build());

                assertEquals(new BigDecimal("116.00000000"), result.getPrice());
    }

    @Test
    void shouldRejectUnsupportedPosition() {
        Player player = Player.builder().position("XX").build();
        StrategyConfig strategyConfig = StrategyConfig.builder()
                .id(52L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("10.00"))
                .version(1)
                .build();

        assertThrows(IllegalArgumentException.class, () -> strategy.evaluate(ValuationContext.builder()
                .player(player)
                .strategyConfig(strategyConfig)
                .build()));
    }
}

