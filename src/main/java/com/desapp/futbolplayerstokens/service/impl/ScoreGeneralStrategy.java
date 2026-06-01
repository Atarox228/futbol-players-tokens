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
import java.util.Objects;
import java.util.Map;

@Service
public class ScoreGeneralStrategy implements Strategy {

    private static final int SCALE = 8;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    // Default weights
    private static final BigDecimal W_GOALS = new BigDecimal("0.25");
    private static final BigDecimal W_ASSISTS = new BigDecimal("0.15");
    private static final BigDecimal W_RATING = new BigDecimal("0.20");
    private static final BigDecimal W_KEYPASSES = new BigDecimal("0.10");
    private static final BigDecimal W_DRIBBLES = new BigDecimal("0.10");
    private static final BigDecimal W_TACKLES = new BigDecimal("0.10");
    private static final BigDecimal W_YELLOW = new BigDecimal("0.05");
    private static final BigDecimal W_RED = new BigDecimal("0.05");

    @Override
    public ValuationResult evaluate(ValuationContext valuationContext) {
        ValuationContext ctx;
        Player p;
        StrategyConfig cfg;
        try {
            ctx = Objects.requireNonNull(valuationContext, "ValuationContext, player and strategyConfig are required");
            p = Objects.requireNonNull(ctx.getPlayer(), "ValuationContext, player and strategyConfig are required");
            cfg = Objects.requireNonNull(ctx.getStrategyConfig(), "ValuationContext, player and strategyConfig are required");
        } catch (NullPointerException ex) {
            throw new ValidationException("ValuationContext, player and strategyConfig are required");
        }
        Map<String, BigDecimal> weights = cfg.getWeights();

        BigDecimal goalsNorm = norm(p.getGoals(), 30.0);
        BigDecimal assistsNorm = norm(p.getAssists(), 20.0);
        BigDecimal ratingNorm = normRating(p.getRating());
        BigDecimal keyPassesNorm = norm(p.getKeyPasses(), 100.0);
        BigDecimal dribblesNorm = norm(p.getDribbles(), 50.0);
        BigDecimal tacklesNorm = norm(p.getTackles(), 80.0);
        BigDecimal yellowNorm = norm(p.getYellowCards(), 10.0);
        BigDecimal redNorm = norm(p.getRedCards(), 3.0);

        BigDecimal positive = getWeight(weights, "goals", W_GOALS).multiply(goalsNorm)
                .add(getWeight(weights, "assists", W_ASSISTS).multiply(assistsNorm))
                .add(getWeight(weights, "rating", W_RATING).multiply(ratingNorm))
                .add(getWeight(weights, "keyPasses", W_KEYPASSES).multiply(keyPassesNorm))
                .add(getWeight(weights, "dribbles", W_DRIBBLES).multiply(dribblesNorm))
                .add(getWeight(weights, "tackles", W_TACKLES).multiply(tacklesNorm));

        BigDecimal negative = getWeight(weights, "yellowCards", W_YELLOW).multiply(yellowNorm)
                .add(getWeight(weights, "redCards", W_RED).multiply(redNorm));

        BigDecimal score = positive.subtract(negative);
        if (score.compareTo(ZERO) < 0) score = ZERO;
        if (score.compareTo(ONE) > 0) score = ONE;

        BigDecimal valorBase = cfg.getValorBase() == null ? ZERO : cfg.getValorBase();
        BigDecimal factor = cfg.getFactorEscala() == null ? ONE : cfg.getFactorEscala();

        BigDecimal price = valorBase.add(score.multiply(factor)).setScale(SCALE, RoundingMode.HALF_UP);

        return ValuationResult.builder()
                .price(price)
                .strategyId(cfg.getId())
                .strategyVersion(cfg.getVersion())
                .build();
    }

    private BigDecimal getWeight(Map<String, BigDecimal> weights, String key, BigDecimal defaultWeight) {
        if (weights == null) return defaultWeight;
        return weights.getOrDefault(key, defaultWeight);
    }

    private BigDecimal norm(Number raw, double divisor) {
        double value = raw == null ? 0.0 : raw.doubleValue();
        if (divisor == 0) return ZERO;
        BigDecimal result = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(divisor), SCALE + 4, RoundingMode.HALF_UP);
        if (result.compareTo(ZERO) < 0) return ZERO;
        if (result.compareTo(ONE) > 0) return ONE;
        return result.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normRating(Double rating) {
        BigDecimal raw = rating == null ? ZERO : new BigDecimal(rating.toString());
        BigDecimal bd = raw.subtract(BigDecimal.valueOf(5.0)).divide(BigDecimal.valueOf(5.0), SCALE + 4, RoundingMode.HALF_UP);
        if (bd.compareTo(ZERO) < 0) return ZERO;
        if (bd.compareTo(ONE) > 0) return ONE;
        return bd.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
