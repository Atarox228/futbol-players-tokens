package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.service.PlayerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerScraperService Tests")
class PlayerScraperServiceImplTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerService playerService;

    @InjectMocks
    private PlayerScraperServiceImpl playerScraperService;

    @Test
    @DisplayName("Debería detectar una página HTTP de error real")
    void testIsLikelyHttpErrorPageWhenTitleIndicatesGatewayError() {
        boolean result = PlayerScraperServiceImpl.isLikelyHttpErrorPage(
                "502 Bad Gateway",
                "<html><body>Service unavailable</body></html>"
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("Debería ignorar páginas normales aunque contengan texto no relacionado")
    void testIsLikelyHttpErrorPageWhenPageIsNormal() {
        boolean result = PlayerScraperServiceImpl.isLikelyHttpErrorPage(
                "Estadísticas de Jugador Ligue 1",
                "<html><body>Accept all <table><tbody><tr><td>Player</td></tr></tbody></table></body></html>"
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Debería ignorar valores nulos")
    void testIsLikelyHttpErrorPageWithNullValues() {
        boolean result = PlayerScraperServiceImpl.isLikelyHttpErrorPage(null, null);

        assertFalse(result);
    }

    @Test
    @DisplayName("Debería parsear enteros y decimales de estadística")
    void testParseStatHelpers() {
        assertEquals(12, PlayerScraperServiceImpl.parseIntegerStat("12"));
        assertEquals(26, PlayerScraperServiceImpl.parseIntegerStat("15(11)"));
        assertEquals(0, PlayerScraperServiceImpl.parseIntegerStat(null));

        assertEquals(87.5, PlayerScraperServiceImpl.parseDecimalStat("87,5%"));
        assertEquals(6.78, PlayerScraperServiceImpl.parseDecimalStat("6.78"));
        assertEquals(0.0, PlayerScraperServiceImpl.parseDecimalStat(""));
    }
}
