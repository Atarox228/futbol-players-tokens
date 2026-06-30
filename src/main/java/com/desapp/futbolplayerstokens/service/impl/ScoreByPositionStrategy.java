package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.ValuationContext;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.exception.ValidationException;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig.StrategyType;
import com.desapp.futbolplayerstokens.service.Strategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Service
public class ScoreByPositionStrategy implements Strategy {

    private final ScoreGeneralStrategy scoreGeneralStrategy;

    public ScoreByPositionStrategy(ScoreGeneralStrategy scoreGeneralStrategy) {
        this.scoreGeneralStrategy = scoreGeneralStrategy;
    }

    private static final int SCALE = 8;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    private static final String KEY_RED_CARDS = "redCards";
    private static final String KEY_YELLOW_CARDS = "yellowCards";
    private static final String KEY_RATING = "rating";

    // Forward defaults
    private static final BigDecimal FWD_GOALS = new BigDecimal("0.35");
    private static final BigDecimal FWD_OWN_GOALS = BigDecimal.ZERO;
    private static final BigDecimal FWD_SHOTS = new BigDecimal("0.20");
    private static final BigDecimal FWD_DRIBBLES = new BigDecimal("0.20");
    private static final BigDecimal FWD_ASSISTS = new BigDecimal("0.15");
    private static final BigDecimal FWD_KEYPASSES = new BigDecimal("0.10");
    private static final BigDecimal FWD_RED = new BigDecimal("0.10");
    private static final BigDecimal FWD_YELLOW = new BigDecimal("0.05");

    // Midfielder defaults
    private static final BigDecimal MID_KEYPASSES = new BigDecimal("0.30");
    private static final BigDecimal MID_PASS_ACCURACY = BigDecimal.ZERO;
    private static final BigDecimal MID_ASSISTS = new BigDecimal("0.25");
    private static final BigDecimal MID_DRIBBLES = new BigDecimal("0.20");
    private static final BigDecimal MID_TACKLES = new BigDecimal("0.15");
    private static final BigDecimal MID_RATING = new BigDecimal("0.10");
    private static final BigDecimal MID_YELLOW = new BigDecimal("0.05");
    private static final BigDecimal MID_RED = new BigDecimal("0.10");

    // Defender defaults
    private static final BigDecimal DEF_TACKLES = new BigDecimal("0.30");
    private static final BigDecimal DEF_INTERCEPTIONS = new BigDecimal("0.25");
    private static final BigDecimal DEF_OWN_GOALS = BigDecimal.ZERO;
    private static final BigDecimal DEF_FAULTS = BigDecimal.ZERO;
    private static final BigDecimal DEF_CLEARS = new BigDecimal("0.20");
    private static final BigDecimal DEF_BLOCKS = new BigDecimal("0.15");
    private static final BigDecimal DEF_RATING = new BigDecimal("0.10");
    private static final BigDecimal DEF_RED = new BigDecimal("0.10");
    private static final BigDecimal DEF_YELLOW = new BigDecimal("0.05");

    // Goalkeeper defaults
    private static final BigDecimal GK_CLEARS = new BigDecimal("0.40");
    private static final BigDecimal GK_BLOCKS = new BigDecimal("0.30");
    private static final BigDecimal GK_INTERCEPTIONS = new BigDecimal("0.20");
    private static final BigDecimal GK_RATING = new BigDecimal("0.10");
    private static final BigDecimal GK_RED = new BigDecimal("0.10");

    @Override
    public ValuationResult evaluate(ValuationContext valuationContext) {
        ValuationContext ctx;
        Player p;
        StrategyConfig cfg;
        try {
            ctx = Objects.requireNonNull(valuationContext, "ValuationContext is required");
            p = Objects.requireNonNull(ctx.getPlayer(), "player is required");
            cfg = Objects.requireNonNull(ctx.getStrategyConfig(), "strategyConfig is required");
        } catch (NullPointerException ex) {
            throw new ValidationException("ValuationContext is required");
        }

        String pos = p.getPosition() == null ? "" : p.getPosition().trim().toUpperCase(java.util.Locale.ROOT);

        BigDecimal score;
        if (isForward(pos)) {
            score = scoreForward(p, cfg.getWeights());
        } else if (isMidfielder(pos)) {
            score = scoreMidfielder(p, cfg.getWeights());
        } else if (isDefender(pos)) {
            score = scoreDefender(p, cfg.getWeights());
        } else if (isGoalkeeper(pos)) {
            score = scoreGoalkeeper(p, cfg.getWeights());
        } else {
            // fallback to general calculation
            ValuationResult r = scoreGeneralStrategy.evaluate(valuationContext);
            return ValuationResult.builder()
                    .price(r.getPrice())
                    .strategyId(cfg.getId())
                    .strategyVersion(cfg.getVersion())
                    .build();
        }

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

    public static StrategyType resolveType(String position) {
        String pos = position == null ? "" : position.trim().toUpperCase(java.util.Locale.ROOT);
        if (pos.isEmpty()) return StrategyType.GENERAL;
        String[] fwVals = new String[]{"FW","ST","CF","SS","LW","RW","WF","LF","RF","FORWARD","DELANTERO","STRIKER"};
        for (String v : fwVals) if (pos.contains(v)) return StrategyType.FORWARD;
        String[] mfVals = new String[]{"MF","MID","AM","CM","DM","CAM","CDM","LM","RM","WM","MEDIOCAMPO","MIDFIELDER"};
        for (String v : mfVals) if (pos.contains(v)) return StrategyType.MIDFIELDER;
        String[] dfVals = new String[]{"DF","DEF","CB","LB","RB","LWB","RWB","SW","DEFENSA","DEFENDER"};
        for (String v : dfVals) if (pos.contains(v)) return StrategyType.DEFENDER;
        String[] gkVals = new String[]{"GK","GKP","POR","PT","GOALKEEPER","ARQUERO"};
        for (String v : gkVals) if (pos.contains(v)) return StrategyType.GOALKEEPER;
        return StrategyType.GENERAL;
    }

    private boolean isForward(String pos) {
        String[] vals = new String[]{"FW","ST","CF","SS","LW","RW","WF","LF","RF","FORWARD","DELANTERO","STRIKER"};
        for (String v : vals) if (pos.contains(v)) return true;
        return false;
    }

    private boolean isMidfielder(String pos) {
        String[] vals = new String[]{"MF","MID","AM","CM","DM","CAM","CDM","LM","RM","WM","MEDIOCAMPO","MIDFIELDER"};
        for (String v : vals) if (pos.contains(v)) return true;
        return false;
    }

    private boolean isDefender(String pos) {
        String[] vals = new String[]{"DF","DEF","CB","LB","RB","LWB","RWB","SW","DEFENSA","DEFENDER"};
        for (String v : vals) if (pos.contains(v)) return true;
        return false;
    }

    private boolean isGoalkeeper(String pos) {
        String[] vals = new String[]{"GK","GKP","POR","PT","GOALKEEPER","ARQUERO"};
        for (String v : vals) if (pos.contains(v)) return true;
        return false;
    }

    private BigDecimal scoreForward(Player p, java.util.Map<String, BigDecimal> weights) {
        BigDecimal goals = norm(p.getGoals(), 30.0);
        BigDecimal ownGoals = norm(p.getOwnGoals(), 30.0);
        BigDecimal shots = norm(p.getShotsOnTarget(), 30.0);
        BigDecimal dribbles = norm(p.getDribbles(), 50.0);
        BigDecimal assists = norm(p.getAssists(), 20.0);
        BigDecimal keyPasses = norm(p.getKeyPasses(), 100.0);
        BigDecimal red = norm(p.getRedCards(), 3.0);
        BigDecimal yellow = norm(p.getYellowCards(), 10.0);

        BigDecimal positive = getW(weights, "goals", FWD_GOALS).multiply(goals)
                .add(getW(weights, "ownGoals", FWD_OWN_GOALS).multiply(ownGoals))
                .add(getW(weights, "shots", FWD_SHOTS).multiply(shots))
                .add(getW(weights, "dribbles", FWD_DRIBBLES).multiply(dribbles))
                .add(getW(weights, "assists", FWD_ASSISTS).multiply(assists))
                .add(getW(weights, "keyPasses", FWD_KEYPASSES).multiply(keyPasses));

        BigDecimal negative = getW(weights, KEY_RED_CARDS, FWD_RED).multiply(red)
                .add(getW(weights, KEY_YELLOW_CARDS, FWD_YELLOW).multiply(yellow));

        return positive.subtract(negative);
    }

    private BigDecimal scoreMidfielder(Player p, java.util.Map<String, BigDecimal> weights) {
        BigDecimal keyPasses = norm(p.getKeyPasses(), 100.0);
        BigDecimal passAccuracy = norm(p.getPassAccuracy(), 1.0);
        BigDecimal assists = norm(p.getAssists(), 20.0);
        BigDecimal dribbles = norm(p.getDribbles(), 50.0);
        BigDecimal tackles = norm(p.getTackles(), 80.0);
        BigDecimal rating = normRating(p.getRating());
        BigDecimal red = norm(p.getRedCards(), 3.0);
        BigDecimal yellow = norm(p.getYellowCards(), 10.0);

        BigDecimal positive = getW(weights, "keyPasses", MID_KEYPASSES).multiply(keyPasses)
                .add(getW(weights, "passAccuracy", MID_PASS_ACCURACY).multiply(passAccuracy))
                .add(getW(weights, "assists", MID_ASSISTS).multiply(assists))
                .add(getW(weights, "dribbles", MID_DRIBBLES).multiply(dribbles))
                .add(getW(weights, "tackles", MID_TACKLES).multiply(tackles))
                .add(getW(weights, KEY_RATING, MID_RATING).multiply(rating));

        BigDecimal negative = getW(weights, KEY_YELLOW_CARDS, MID_YELLOW).multiply(yellow)
                .add(getW(weights, KEY_RED_CARDS, MID_RED).multiply(red));

        return positive.subtract(negative);
    }

    private BigDecimal scoreDefender(Player p, java.util.Map<String, BigDecimal> weights) {
        BigDecimal tackles = norm(p.getTackles(), 80.0);
        BigDecimal interceptions = norm(p.getInterceptions(), 60.0);
        BigDecimal ownGoals = norm(p.getOwnGoals(), 5.0);
        BigDecimal faults = norm(p.getFaults(), 40.0);
        BigDecimal clears = norm(p.getClears(), 60.0);
        BigDecimal blocks = norm(p.getBlocks(), 30.0);
        BigDecimal rating = normRating(p.getRating());
        BigDecimal red = norm(p.getRedCards(), 3.0);
        BigDecimal yellow = norm(p.getYellowCards(), 10.0);

        BigDecimal positive = getW(weights, "tackles", DEF_TACKLES).multiply(tackles)
                .add(getW(weights, "interceptions", DEF_INTERCEPTIONS).multiply(interceptions))
                .add(getW(weights, "clears", DEF_CLEARS).multiply(clears))
                .add(getW(weights, "blocks", DEF_BLOCKS).multiply(blocks))
                .add(getW(weights, KEY_RATING, DEF_RATING).multiply(rating));

        BigDecimal negative = getW(weights, KEY_RED_CARDS, DEF_RED).multiply(red)
                .add(getW(weights, KEY_YELLOW_CARDS, DEF_YELLOW).multiply(yellow))
                .add(getW(weights, "ownGoals", DEF_OWN_GOALS).multiply(ownGoals))
                .add(getW(weights, "faults", DEF_FAULTS).multiply(faults));

        return positive.subtract(negative);
    }

    private BigDecimal scoreGoalkeeper(Player p, java.util.Map<String, BigDecimal> weights) {
        BigDecimal clears = norm(p.getClears(), 40.0);
        BigDecimal blocks = norm(p.getBlocks(), 20.0);
        BigDecimal interceptions = norm(p.getInterceptions(), 30.0);
        BigDecimal rating = normRating(p.getRating());
        BigDecimal red = norm(p.getRedCards(), 3.0);

        BigDecimal positive = getW(weights, "clears", GK_CLEARS).multiply(clears)
                .add(getW(weights, "blocks", GK_BLOCKS).multiply(blocks))
                .add(getW(weights, "interceptions", GK_INTERCEPTIONS).multiply(interceptions))
                .add(getW(weights, KEY_RATING, GK_RATING).multiply(rating));

        BigDecimal negative = getW(weights, KEY_RED_CARDS, GK_RED).multiply(red);

        return positive.subtract(negative);
    }

    private BigDecimal getW(java.util.Map<String, BigDecimal> weights, String key, BigDecimal def) {
        if (weights == null) return def;
        return weights.getOrDefault(key, def);
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
        BigDecimal result = raw.subtract(BigDecimal.valueOf(5.0)).divide(BigDecimal.valueOf(5.0), SCALE + 4, RoundingMode.HALF_UP);
        if (result.compareTo(ZERO) < 0) return ZERO;
        if (result.compareTo(ONE) > 0) return ONE;
        return result.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
