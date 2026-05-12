package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.ValuationContext;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.exception.ValidationException;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.service.Strategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class ScoreGeneralStrategy implements Strategy {

    private static final int SCALE = 8;
    private static final BigDecimal DEFAULT_WEIGHT_APPEARANCES = new BigDecimal("0.20");
    private static final BigDecimal DEFAULT_WEIGHT_MINUTES = new BigDecimal("0.15");
    private static final BigDecimal DEFAULT_WEIGHT_RATING = new BigDecimal("0.20");
    private static final BigDecimal DEFAULT_WEIGHT_YELLOW = new BigDecimal("0.10");
    private static final BigDecimal DEFAULT_WEIGHT_RED = new BigDecimal("0.20");
    private static final BigDecimal NINETY = BigDecimal.valueOf(90);
    private static final BigDecimal SEVEN = BigDecimal.valueOf(7);
    private static final BigDecimal THREE = BigDecimal.valueOf(3);
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    @Override
    public ValuationResult evaluate(ValuationContext valuationContext) {
        if (valuationContext == null || valuationContext.getPlayer() == null || valuationContext.getStrategyConfig() == null) {
            throw new ValidationException("ValuationContext, player and strategyConfig are required");
        }

        Player player = valuationContext.getPlayer();
        StrategyConfig strategyConfig = valuationContext.getStrategyConfig();
        Map<String, BigDecimal> weights = strategyConfig.getWeights();

        BigDecimal scoreTotal = ZERO
            .add(weighted(weight(weights, "appearances", DEFAULT_WEIGHT_APPEARANCES), numberValue(player.getAppearances())))
                .add(weighted(weight(weights, "minutes", DEFAULT_WEIGHT_MINUTES), minutesComponent(player)))
                .add(weighted(weight(weights, "rating", DEFAULT_WEIGHT_RATING), ratingComponent(player)))
            .subtract(weighted(weight(weights, "yellowCards", DEFAULT_WEIGHT_YELLOW), numberValue(player.getYellowCards())))
            .subtract(weighted(weight(weights, "redCards", DEFAULT_WEIGHT_RED), numberValue(player.getRedCards())));

        BigDecimal price = nullToZero(strategyConfig.getValorBase())
                .add(scoreTotal.multiply(nullToOne(strategyConfig.getFactorEscala())))
                .setScale(SCALE, RoundingMode.HALF_UP);

        return ValuationResult.builder()
                .price(price)
                .strategyId(strategyConfig.getId())
                .strategyVersion(strategyConfig.getVersion())
                .build();
    }

    private BigDecimal weighted(BigDecimal weight, BigDecimal value) {
        return weight.multiply(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal minutesComponent(Player player) {
        return numberValue(player.getMinutes()).divide(NINETY, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal ratingComponent(Player player) {
        BigDecimal rating = player.getRating() == null ? ONE : BigDecimal.valueOf(player.getRating());
        BigDecimal normalized = rating.subtract(SEVEN).divide(THREE, SCALE, RoundingMode.HALF_UP);
        return normalized.compareTo(ZERO) > 0 ? normalized : ZERO;
    }

    private BigDecimal numberValue(Number value) {
        return BigDecimal.valueOf(value == null ? 1.0 : value.doubleValue());
    }

    private BigDecimal weight(Map<String, BigDecimal> weights, String key, BigDecimal defaultWeight) {
        if (weights == null) {
            return defaultWeight;
        }
        return weights.getOrDefault(key, defaultWeight);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal nullToOne(BigDecimal value) {
        return value == null ? ONE : value;
    }
}

