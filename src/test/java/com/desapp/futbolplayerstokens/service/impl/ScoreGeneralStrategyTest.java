package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.ValuationContext;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.exception.ValidationException;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreGeneralStrategyTest {

    private final ScoreGeneralStrategy strategy = new ScoreGeneralStrategy();

    @Test
    void shouldCalculatePlayerPriceUsingConfiguredWeights() {
        Map<String, BigDecimal> weights = new HashMap<>();
        weights.put("appearances", new BigDecimal("0.20"));
        weights.put("won", new BigDecimal("0.00"));
        weights.put("lost", new BigDecimal("0.00"));
        weights.put("minutes", new BigDecimal("0.15"));
        weights.put("rating", new BigDecimal("0.20"));
        weights.put("yellowCards", new BigDecimal("0.10"));
        weights.put("redCards", new BigDecimal("0.20"));

        Player player = Player.builder()
                .id(1L)
            .appearances(10)
                .minutes(900)
                .rating(8.5)
                .yellowCards(1)
                .redCards(0)
                .build();

        StrategyConfig strategyConfig = StrategyConfig.builder()
                .id(42L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("50.00"))
                .version(3)
                .weights(weights)
                .build();

        ValuationResult result = strategy.evaluate(ValuationContext.builder()
                .player(player)
                .strategyConfig(strategyConfig)
                .build());

            assertEquals(new BigDecimal("275.00000000"), result.getPrice());
        assertEquals(42L, result.getStrategyId());
        assertEquals(3, result.getStrategyVersion());
    }

    @Test
    void shouldRejectNullContext() {
        assertThrows(ValidationException.class, () -> strategy.evaluate(null));
    }
}

