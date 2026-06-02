package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.ValuationContext;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.exception.ValidationException;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreGeneralStrategyTest {

    private final ScoreGeneralStrategy strategy = new ScoreGeneralStrategy();

    @Test
    void shouldCalculatePlayerPriceUsingDefaults() {
        Player player = Player.builder()
                .id(1L)
                .goals(10)
                .assists(5)
                .rating(8.0)
                .yellowCards(1)
                .redCards(0)
                .tackles(20.0)
                .keyPasses(30.0)
                .dribbles(15.0)
                .build();

        StrategyConfig cfg = StrategyConfig.builder()
                .id(7L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("50.00"))
                .version(1)
                .weights(new HashMap<>())
                .build();

        ValuationResult res = strategy.evaluate(ValuationContext.builder().player(player).strategyConfig(cfg).build());

        // calcular expected siguiendo la misma regla: price = valorBase + score * factorEscala
        BigDecimal goalsNorm = new BigDecimal("10").divide(new BigDecimal("30"), 8, RoundingMode.HALF_UP);
        BigDecimal assistsNorm = new BigDecimal("5").divide(new BigDecimal("20"), 8, RoundingMode.HALF_UP);
        BigDecimal ratingNorm = BigDecimal.valueOf((8.0 - 5.0) / 5.0).setScale(8, RoundingMode.HALF_UP);
        BigDecimal keyPassesNorm = new BigDecimal("30").divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
        BigDecimal dribblesNorm = new BigDecimal("15").divide(new BigDecimal("50"), 8, RoundingMode.HALF_UP);
        BigDecimal tacklesNorm = new BigDecimal("20").divide(new BigDecimal("80"), 8, RoundingMode.HALF_UP);
        BigDecimal yellowNorm = new BigDecimal("1").divide(new BigDecimal("10"), 8, RoundingMode.HALF_UP);
        BigDecimal redNorm = BigDecimal.ZERO;

        BigDecimal positive = new BigDecimal("0.25").multiply(goalsNorm)
                .add(new BigDecimal("0.15").multiply(assistsNorm))
                .add(new BigDecimal("0.20").multiply(ratingNorm))
                .add(new BigDecimal("0.10").multiply(keyPassesNorm))
                .add(new BigDecimal("0.10").multiply(dribblesNorm))
                .add(new BigDecimal("0.10").multiply(tacklesNorm));

        BigDecimal negative = new BigDecimal("0.05").multiply(yellowNorm)
                .add(new BigDecimal("0.05").multiply(redNorm));

        BigDecimal score = positive.subtract(negative);
        if (score.compareTo(BigDecimal.ZERO) < 0) score = BigDecimal.ZERO;
        if (score.compareTo(BigDecimal.ONE) > 0) score = BigDecimal.ONE;

        BigDecimal expected = cfg.getValorBase().add(score.multiply(cfg.getFactorEscala())).setScale(8, RoundingMode.HALF_UP);
        assertEquals(expected, res.getPrice());
        assertEquals(7L, res.getStrategyId());
        assertEquals(1, res.getStrategyVersion());
    }

    @Test
    void shouldReturnValorBaseWhenAllStatsNull() {
        Player player = Player.builder().id(2L).build();
        StrategyConfig cfg = StrategyConfig.builder()
                .id(8L)
                .valorBase(new BigDecimal("200.00"))
                .factorEscala(new BigDecimal("100.00"))
                .version(1)
                .build();

        ValuationResult res = strategy.evaluate(ValuationContext.builder().player(player).strategyConfig(cfg).build());
        assertEquals(new BigDecimal("200.00000000"), res.getPrice());
    }

    @Test
    void shouldClampNegativeScoreToZero() {
        Player player = Player.builder()
                .id(3L)
                .yellowCards(100)
                .redCards(10)
                .build();

        StrategyConfig cfg = StrategyConfig.builder()
                .id(9L)
                .valorBase(new BigDecimal("50.00"))
                .factorEscala(new BigDecimal("20.00"))
                .version(1)
                .build();

        ValuationResult res = strategy.evaluate(ValuationContext.builder().player(player).strategyConfig(cfg).build());
        // negative impacts but clamped -> price == valorBase
        assertEquals(new BigDecimal("50.00000000"), res.getPrice());
    }

    @Test
    void shouldClampScoreToOneWhenWeightsPushItAboveOne() {
        Player player = Player.builder()
                .id(4L)
                .goals(30)
                .assists(20)
                .rating(10.0)
                .yellowCards(0)
                .redCards(0)
                .tackles(80.0)
                .keyPasses(100.0)
                .dribbles(50.0)
                .build();

        HashMap<String, BigDecimal> weights = new HashMap<>();
        weights.put("goals", new BigDecimal("5.00"));

        StrategyConfig cfg = StrategyConfig.builder()
                .id(10L)
                .valorBase(new BigDecimal("50.00"))
                .factorEscala(new BigDecimal("20.00"))
                .version(1)
                .weights(weights)
                .build();

        ValuationResult res = strategy.evaluate(ValuationContext.builder().player(player).strategyConfig(cfg).build());
        assertEquals(new BigDecimal("70.00000000"), res.getPrice());
    }

    @Test
    void shouldUseConfiguredMinutesAndRatingOnly() {
        Player player = Player.builder()
                .id(5L)
                .minutes(1710)
                .rating(7.5)
                .yellowCards(0)
                .redCards(0)
                .build();

        HashMap<String, BigDecimal> weights = new HashMap<>();
        weights.put("goals", BigDecimal.ZERO);
        weights.put("assists", BigDecimal.ZERO);
        weights.put("keyPasses", BigDecimal.ZERO);
        weights.put("dribbles", BigDecimal.ZERO);
        weights.put("tackles", BigDecimal.ZERO);
        weights.put("minutes", new BigDecimal("0.50"));
        weights.put("rating", new BigDecimal("0.50"));
        weights.put("yellowCards", new BigDecimal("0.20"));
        weights.put("redCards", new BigDecimal("0.40"));

        StrategyConfig cfg = StrategyConfig.builder()
                .id(11L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("10.00"))
                .version(1)
                .weights(weights)
                .build();

        ValuationResult res = strategy.evaluate(ValuationContext.builder().player(player).strategyConfig(cfg).build());

        assertEquals(new BigDecimal("105.00000000"), res.getPrice());
    }

    @Test
    void shouldRejectNullContext() {
        assertThrows(ValidationException.class, () -> strategy.evaluate(null));
    }
}

