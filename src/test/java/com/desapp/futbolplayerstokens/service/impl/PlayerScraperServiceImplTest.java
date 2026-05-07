package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.service.PlayerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
