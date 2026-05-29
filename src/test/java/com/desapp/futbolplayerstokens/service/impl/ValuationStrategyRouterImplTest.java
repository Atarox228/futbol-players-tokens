package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValuationStrategyRouterImplTest {

    private final ScoreGeneralStrategy scoreGeneralStrategy = new ScoreGeneralStrategy();
    private final ScoreByPositionStrategy scoreByPositionStrategy = new ScoreByPositionStrategy();
    private final ValuationStrategyRouterImpl router = new ValuationStrategyRouterImpl(scoreGeneralStrategy, scoreByPositionStrategy);

    @Test
    void shouldReturnGeneralStrategyWhenKeyIsNullOrBlank() {
        assertSame(scoreGeneralStrategy, router.resolve(null));
        assertSame(scoreGeneralStrategy, router.resolve("   "));
    }

    @Test
    void shouldReturnPositionStrategyWhenExplicitKeyIsPassed() {
        assertSame(scoreByPositionStrategy, router.resolve("POSITION"));
        assertSame(scoreByPositionStrategy, router.resolve("by_position"));
    }

    @Test
    void shouldRejectUnsupportedKey() {
        assertThrows(ValidationException.class, () -> router.resolve("unknown"));
    }
}

