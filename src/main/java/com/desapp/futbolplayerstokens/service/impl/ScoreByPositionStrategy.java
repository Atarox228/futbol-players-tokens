package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.ValuationContext;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.service.Strategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class ScoreByPositionStrategy implements Strategy {

    private static final int SCALE = 8;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    @Override
    public ValuationResult evaluate(ValuationContext valuationContext) {
        if (valuationContext == null) {
            throw new IllegalArgumentException("ValuationContext is required");
        }

        Player player = valuationContext.getPlayer();
        StrategyConfig strategyConfig = valuationContext.getStrategyConfig();
        if (player == null || strategyConfig == null) {
            throw new IllegalArgumentException("player and strategyConfig are required");
        }
        Map<String, BigDecimal> weights = strategyConfig.getWeights();
        String position = player.getPosition() == null ? "" : player.getPosition().trim().toUpperCase();

        BigDecimal scoreTotal = switch (position) {
            case "FW", "FWD", "ST" -> scoreForward(player, weights);
            case "GK" -> scoreGoalkeeper(player, weights);
            case "DF", "DEF" -> scoreDefender(player, weights);
            case "MF", "MID" -> scoreMidfielder(player, weights);
            default -> throw new IllegalArgumentException("Unsupported position for position strategy: " + player.getPosition());
        };

        BigDecimal price = nullToZero(strategyConfig.getValorBase())
                .add(scoreTotal.multiply(nullToOne(strategyConfig.getFactorEscala())))
                .setScale(SCALE, RoundingMode.HALF_UP);

        return ValuationResult.builder()
                .price(price)
                .strategyId(strategyConfig.getId())
                .strategyVersion(strategyConfig.getVersion())
                .build();
    }

    private BigDecimal scoreForward(Player player, Map<String, BigDecimal> weights) {
        return weighted(weights, "goals", new BigDecimal("0.60"), numberValue(player.getGoals()))
            .add(weighted(weights, "shotsOnTarget", new BigDecimal("0.40"), numberValue(player.getShotsOnTarget())));
    }

    private BigDecimal scoreGoalkeeper(Player player, Map<String, BigDecimal> weights) {
        return weighted(weights, "clears", new BigDecimal("0.40"), numberValue(player.getClears()));
    }

    private BigDecimal scoreDefender(Player player, Map<String, BigDecimal> weights) {
        return weighted(weights, "tackles", new BigDecimal("0.25"), numberValue(player.getTackles()))
            .add(weighted(weights, "interceptions", new BigDecimal("0.25"), numberValue(player.getInterceptions())))
            .add(weighted(weights, "clears", new BigDecimal("0.20"), numberValue(player.getClears())))
            .add(weighted(weights, "blocks", new BigDecimal("0.20"), numberValue(player.getBlocks())))
            .subtract(weighted(weights, "ownGoals", new BigDecimal("0.50"), numberValue(player.getOwnGoals())));
    }

    private BigDecimal scoreMidfielder(Player player, Map<String, BigDecimal> weights) {
        return weighted(weights, "assists", new BigDecimal("0.40"), numberValue(player.getAssists()))
            .add(weighted(weights, "keyPasses", new BigDecimal("0.35"), numberValue(player.getKeyPasses())))
                .add(weighted(weights, "passAccuracy", new BigDecimal("0.25"), passAccuracyValue(player)));
    }

    private BigDecimal weighted(Map<String, BigDecimal> weights, String key, BigDecimal defaultWeight, BigDecimal value) {
        return weight(weights, key, defaultWeight).multiply(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal weight(Map<String, BigDecimal> weights, String key, BigDecimal defaultWeight) {
        if (weights == null) {
            return defaultWeight;
        }
        return weights.getOrDefault(key, defaultWeight);
    }

    private BigDecimal numberValue(Number value) {
        return BigDecimal.valueOf(value == null ? 1.0 : value.doubleValue());
    }

    private BigDecimal passAccuracyValue(Player player) {
        return BigDecimal.valueOf(player.getPassAccuracy() == null ? 1.0 : player.getPassAccuracy());
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal nullToOne(BigDecimal value) {
        return value == null ? ONE : value;
    }
}


