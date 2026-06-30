package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValuationStrategyRouterImplTest {

    private final ScoreGeneralStrategy scoreGeneralStrategy = new ScoreGeneralStrategy();
    private final ScoreByPositionStrategy scoreByPositionStrategy = new ScoreByPositionStrategy(scoreGeneralStrategy);
    private final ValuationStrategyRouterImpl router = new ValuationStrategyRouterImpl(scoreGeneralStrategy, scoreByPositionStrategy);

    @Test
    void shouldReturnGeneralStrategyWhenKeyIsNullOrBlankOrGeneralVariants() {
        assertSame(scoreGeneralStrategy, router.resolve(null));
        assertSame(scoreGeneralStrategy, router.resolve("   "));
        assertSame(scoreGeneralStrategy, router.resolve("GENERAL"));
        assertSame(scoreGeneralStrategy, router.resolve("default"));
        assertSame(scoreGeneralStrategy, router.resolve("General_Score"));
    }

    @Test
    void shouldReturnPositionStrategyWhenExplicitKeyIsPassed() {
        assertSame(scoreByPositionStrategy, router.resolve("POSITION"));
        assertSame(scoreByPositionStrategy, router.resolve("by_position"));
        assertSame(scoreByPositionStrategy, router.resolve("POSITION_SCORE"));
    }

    @Test
    void shouldRejectUnsupportedKey() {
        assertThrows(ValidationException.class, () -> router.resolve("unknown"));
    }
}
