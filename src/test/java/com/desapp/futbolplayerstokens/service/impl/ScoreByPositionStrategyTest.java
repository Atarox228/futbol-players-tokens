package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.ValuationContext;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreByPositionStrategyTest {

    private final ScoreGeneralStrategy general = new ScoreGeneralStrategy();
    private final ScoreByPositionStrategy strategy = createStrategy();

    private ScoreByPositionStrategy createStrategy() {
        ScoreByPositionStrategy s = new ScoreByPositionStrategy();
        s.setScoreGeneralStrategy(general);
        return s;
    }

    @Test
    void forwardWithMoreGoalsScoresHigher() {
        StrategyConfig cfg = StrategyConfig.builder()
                .id(50L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("10.00"))
                .version(1)
                .weights(new HashMap<>())
                .build();

        Player fwHigh = Player.builder().position("FW").goals(20).build();
        Player fwLow = Player.builder().position("FW").goals(0).build();

        ValuationResult rHigh = strategy.evaluate(ValuationContext.builder().player(fwHigh).strategyConfig(cfg).build());
        ValuationResult rLow = strategy.evaluate(ValuationContext.builder().player(fwLow).strategyConfig(cfg).build());

        assertTrue(rHigh.getPrice().compareTo(rLow.getPrice()) > 0);
    }

    @Test
    void forwardUsesForwardFormulaWithExactPriceOnMaxStats() {
        StrategyConfig cfg = StrategyConfig.builder()
                .id(53L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("10.00"))
                .version(1)
                .weights(new HashMap<>())
                .build();

        Player fw = Player.builder()
                .position("  fw  ")
                .goals(30)
                .shotsOnTarget(30.0)
                .dribbles(50.0)
                .assists(20)
                .keyPasses(100.0)
                .redCards(0)
                .yellowCards(0)
                .build();

        ValuationResult res = strategy.evaluate(ValuationContext.builder().player(fw).strategyConfig(cfg).build());
        assertEquals(new BigDecimal("110.00000000"), res.getPrice());
    }

    @Test
    void midfielderUsesMidfielderFormulaWithExactPriceOnMaxStats() {
        StrategyConfig cfg = StrategyConfig.builder()
                .id(54L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("10.00"))
                .version(1)
                .weights(new HashMap<>())
                .build();

        Player mf = Player.builder()
                .position("cm")
                .keyPasses(100.0)
                .assists(20)
                .dribbles(50.0)
                .tackles(80.0)
                .rating(10.0)
                .yellowCards(0)
                .redCards(0)
                .build();

        ValuationResult res = strategy.evaluate(ValuationContext.builder().player(mf).strategyConfig(cfg).build());
        assertEquals(new BigDecimal("110.00000000"), res.getPrice());
    }

    @Test
    void defenderUsesDefenderFormulaWithExactPriceOnMaxStats() {
        StrategyConfig cfg = StrategyConfig.builder()
                .id(55L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("10.00"))
                .version(1)
                .weights(new HashMap<>())
                .build();

        Player df = Player.builder()
                .position("cb")
                .tackles(80.0)
                .interceptions(60.0)
                .clears(60.0)
                .blocks(30.0)
                .rating(10.0)
                .yellowCards(0)
                .redCards(0)
                .build();

        ValuationResult res = strategy.evaluate(ValuationContext.builder().player(df).strategyConfig(cfg).build());
        assertEquals(new BigDecimal("110.00000000"), res.getPrice());
    }

    @Test
    void goalkeeperUsesClearsNotGoals() {
        StrategyConfig cfg = StrategyConfig.builder()
                .id(51L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("10.00"))
                .version(1)
                .weights(new HashMap<>())
                .build();

        Player gkClears = Player.builder().position("GK").clears(10.0).goals(0).build();
        Player gkGoals = Player.builder().position("GK").goals(10).clears(0.0).build();

        ValuationResult rClears = strategy.evaluate(ValuationContext.builder().player(gkClears).strategyConfig(cfg).build());
        ValuationResult rGoals = strategy.evaluate(ValuationContext.builder().player(gkGoals).strategyConfig(cfg).build());

        assertTrue(rClears.getPrice().compareTo(rGoals.getPrice()) > 0);
    }

    @Test
    void goalkeeperUsesGoalkeeperFormulaWithExactPriceOnMaxStats() {
        StrategyConfig cfg = StrategyConfig.builder()
                .id(56L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("10.00"))
                .version(1)
                .weights(new HashMap<>())
                .build();

        Player gk = Player.builder()
                .position("  por ")
                .clears(40.0)
                .blocks(20.0)
                .interceptions(30.0)
                .rating(10.0)
                .redCards(0)
                .build();

        ValuationResult res = strategy.evaluate(ValuationContext.builder().player(gk).strategyConfig(cfg).build());
        assertEquals(new BigDecimal("110.00000000"), res.getPrice());
    }

    @Test
    void unknownOrNullPositionFallsBackToGeneral() {
        StrategyConfig cfg = StrategyConfig.builder()
                .id(52L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("10.00"))
                .version(1)
                .weights(new HashMap<>())
                .build();

        Player pUnknown = Player.builder().position("XX").goals(5).assists(2).build();
        Player pNull = Player.builder().position(null).goals(5).assists(2).build();

        ValuationResult rUnknown = strategy.evaluate(ValuationContext.builder().player(pUnknown).strategyConfig(cfg).build());
        ValuationResult rGeneralFromStrategy = general.evaluate(ValuationContext.builder().player(pUnknown).strategyConfig(cfg).build());

        assertEquals(rGeneralFromStrategy.getPrice(), rUnknown.getPrice());

        ValuationResult rNull = strategy.evaluate(ValuationContext.builder().player(pNull).strategyConfig(cfg).build());
        ValuationResult rGeneralNull = general.evaluate(ValuationContext.builder().player(pNull).strategyConfig(cfg).build());
        assertEquals(rGeneralNull.getPrice(), rNull.getPrice());
    }

    @Test
    void blankPositionFallsBackToGeneral() {
        StrategyConfig cfg = StrategyConfig.builder()
                .id(57L)
                .valorBase(new BigDecimal("100.00"))
                .factorEscala(new BigDecimal("10.00"))
                .version(1)
                .weights(new HashMap<>())
                .build();

        Player pBlank = Player.builder().position("   ").goals(4).assists(3).build();

        ValuationResult rBlank = strategy.evaluate(ValuationContext.builder().player(pBlank).strategyConfig(cfg).build());
        ValuationResult rGeneral = general.evaluate(ValuationContext.builder().player(pBlank).strategyConfig(cfg).build());

        assertEquals(rGeneral.getPrice(), rBlank.getPrice());
    }
}
