package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.service.Strategy;
import com.desapp.futbolplayerstokens.service.ValuationStrategyRouter;
import org.springframework.stereotype.Service;

@Service
public class ValuationStrategyRouterImpl implements ValuationStrategyRouter {

    private static final String POSITION_STRATEGY = "POSITION";

    private final ScoreGeneralStrategy scoreGeneralStrategy;
    private final ScoreByPositionStrategy scoreByPositionStrategy;

    public ValuationStrategyRouterImpl(ScoreGeneralStrategy scoreGeneralStrategy,
                                       ScoreByPositionStrategy scoreByPositionStrategy) {
        this.scoreGeneralStrategy = scoreGeneralStrategy;
        this.scoreByPositionStrategy = scoreByPositionStrategy;
    }

    @Override
    public Strategy resolve(String strategyKey) {
        if (strategyKey == null || strategyKey.trim().isEmpty()) {
            return scoreGeneralStrategy;
        }

        String normalized = strategyKey.trim().toUpperCase();
        return switch (normalized) {
            case "GENERAL", "DEFAULT", "GENERAL_SCORE" -> scoreGeneralStrategy;
            case POSITION_STRATEGY, "BY_POSITION", "POSITION_SCORE" -> scoreByPositionStrategy;
            default -> throw new IllegalArgumentException("Unsupported strategy key: " + strategyKey);
        };
    }
}

