package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.ValuationContext;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.exception.ValidationException;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.service.Strategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Service
//noinspection SpringJavaInjectionPointsAutowiringInspection
public class ScoreByPositionStrategy implements Strategy {

    private ScoreGeneralStrategy scoreGeneralStrategy;

    @Autowired
    public void setScoreGeneralStrategy(ScoreGeneralStrategy scoreGeneralStrategy) {
        this.scoreGeneralStrategy = scoreGeneralStrategy;
    }

    private static final int SCALE = 8;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    // Forward defaults
    private static final BigDecimal FW_GOALS = new BigDecimal("0.35");
    private static final BigDecimal FW_SHOTS = new BigDecimal("0.20");
    private static final BigDecimal FW_DRIBBLES = new BigDecimal("0.20");
    private static final BigDecimal FW_ASSISTS = new BigDecimal("0.15");
    private static final BigDecimal FW_KEYPASSES = new BigDecimal("0.10");
    private static final BigDecimal FW_RED = new BigDecimal("0.10");
    private static final BigDecimal FW_YELLOW = new BigDecimal("0.05");

    // Midfielder defaults
    private static final BigDecimal MF_KEYPASSES = new BigDecimal("0.30");
    private static final BigDecimal MF_ASSISTS = new BigDecimal("0.25");
    private static final BigDecimal MF_DRIBBLES = new BigDecimal("0.20");
    private static final BigDecimal MF_TACKLES = new BigDecimal("0.15");
    private static final BigDecimal MF_RATING = new BigDecimal("0.10");
    private static final BigDecimal MF_YELLOW = new BigDecimal("0.05");
    private static final BigDecimal MF_RED = new BigDecimal("0.10");

    // Defender defaults
    private static final BigDecimal DF_TACKLES = new BigDecimal("0.30");
    private static final BigDecimal DF_INTERCEPTIONS = new BigDecimal("0.25");
    private static final BigDecimal DF_CLEARS = new BigDecimal("0.20");
    private static final BigDecimal DF_BLOCKS = new BigDecimal("0.15");
    private static final BigDecimal DF_RATING = new BigDecimal("0.10");
    private static final BigDecimal DF_RED = new BigDecimal("0.10");
    private static final BigDecimal DF_YELLOW = new BigDecimal("0.05");

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

    private boolean isForward(String pos) {
        String[] vals = new String[]{"FW","ST","CF","SS","LW","RW","WF","LF","RF"};
        for (String v : vals) if (pos.contains(v)) return true;
        return false;
    }

    private boolean isMidfielder(String pos) {
        String[] vals = new String[]{"MF","MID","AM","CM","DM","CAM","CDM","LM","RM","WM"};
        for (String v : vals) if (pos.contains(v)) return true;
        return false;
    }

    private boolean isDefender(String pos) {
        String[] vals = new String[]{"DF","DEF","CB","LB","RB","LWB","RWB","SW"};
        for (String v : vals) if (pos.contains(v)) return true;
        return false;
    }

    private boolean isGoalkeeper(String pos) {
        String[] vals = new String[]{"GK","GKP","POR","PT"};
        for (String v : vals) if (pos.contains(v)) return true;
        return false;
    }

    private BigDecimal scoreForward(Player p, java.util.Map<String, BigDecimal> weights) {
        BigDecimal goals = norm(p.getGoals(), 30.0);
        BigDecimal shots = norm(p.getShotsOnTarget(), 30.0);
        BigDecimal dribbles = norm(p.getDribbles(), 50.0);
        BigDecimal assists = norm(p.getAssists(), 20.0);
        BigDecimal keyPasses = norm(p.getKeyPasses(), 100.0);
        BigDecimal red = norm(p.getRedCards(), 3.0);
        BigDecimal yellow = norm(p.getYellowCards(), 10.0);

        BigDecimal positive = getW(weights, "fw_goals", FW_GOALS).multiply(goals)
                .add(getW(weights, "fw_shots", FW_SHOTS).multiply(shots))
                .add(getW(weights, "fw_dribbles", FW_DRIBBLES).multiply(dribbles))
                .add(getW(weights, "fw_assists", FW_ASSISTS).multiply(assists))
                .add(getW(weights, "fw_keyPasses", FW_KEYPASSES).multiply(keyPasses));

        BigDecimal negative = getW(weights, "fw_redCards", FW_RED).multiply(red)
                .add(getW(weights, "fw_yellowCards", FW_YELLOW).multiply(yellow));

        return positive.subtract(negative);
    }

    private BigDecimal scoreMidfielder(Player p, java.util.Map<String, BigDecimal> weights) {
        BigDecimal keyPasses = norm(p.getKeyPasses(), 100.0);
        BigDecimal assists = norm(p.getAssists(), 20.0);
        BigDecimal dribbles = norm(p.getDribbles(), 50.0);
        BigDecimal tackles = norm(p.getTackles(), 80.0);
        BigDecimal rating = normRating(p.getRating());
        BigDecimal red = norm(p.getRedCards(), 3.0);
        BigDecimal yellow = norm(p.getYellowCards(), 10.0);

        BigDecimal positive = getW(weights, "mf_keyPasses", MF_KEYPASSES).multiply(keyPasses)
                .add(getW(weights, "mf_assists", MF_ASSISTS).multiply(assists))
                .add(getW(weights, "mf_dribbles", MF_DRIBBLES).multiply(dribbles))
                .add(getW(weights, "mf_tackles", MF_TACKLES).multiply(tackles))
                .add(getW(weights, "mf_rating", MF_RATING).multiply(rating));

        BigDecimal negative = getW(weights, "mf_yellowCards", MF_YELLOW).multiply(yellow)
                .add(getW(weights, "mf_redCards", MF_RED).multiply(red));

        return positive.subtract(negative);
    }

    private BigDecimal scoreDefender(Player p, java.util.Map<String, BigDecimal> weights) {
        BigDecimal tackles = norm(p.getTackles(), 80.0);
        BigDecimal interceptions = norm(p.getInterceptions(), 60.0);
        BigDecimal clears = norm(p.getClears(), 60.0);
        BigDecimal blocks = norm(p.getBlocks(), 30.0);
        BigDecimal rating = normRating(p.getRating());
        BigDecimal red = norm(p.getRedCards(), 3.0);
        BigDecimal yellow = norm(p.getYellowCards(), 10.0);

        BigDecimal positive = getW(weights, "df_tackles", DF_TACKLES).multiply(tackles)
                .add(getW(weights, "df_interceptions", DF_INTERCEPTIONS).multiply(interceptions))
                .add(getW(weights, "df_clears", DF_CLEARS).multiply(clears))
                .add(getW(weights, "df_blocks", DF_BLOCKS).multiply(blocks))
                .add(getW(weights, "df_rating", DF_RATING).multiply(rating));

        BigDecimal negative = getW(weights, "df_redCards", DF_RED).multiply(red)
                .add(getW(weights, "df_yellowCards", DF_YELLOW).multiply(yellow));

        return positive.subtract(negative);
    }

    private BigDecimal scoreGoalkeeper(Player p, java.util.Map<String, BigDecimal> weights) {
        BigDecimal clears = norm(p.getClears(), 40.0);
        BigDecimal blocks = norm(p.getBlocks(), 20.0);
        BigDecimal interceptions = norm(p.getInterceptions(), 30.0);
        BigDecimal rating = normRating(p.getRating());
        BigDecimal red = norm(p.getRedCards(), 3.0);

        BigDecimal positive = getW(weights, "gk_clears", GK_CLEARS).multiply(clears)
                .add(getW(weights, "gk_blocks", GK_BLOCKS).multiply(blocks))
                .add(getW(weights, "gk_interceptions", GK_INTERCEPTIONS).multiply(interceptions))
                .add(getW(weights, "gk_rating", GK_RATING).multiply(rating));

        BigDecimal negative = getW(weights, "gk_redCards", GK_RED).multiply(red);

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
