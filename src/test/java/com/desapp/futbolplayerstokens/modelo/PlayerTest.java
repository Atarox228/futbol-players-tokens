package com.desapp.futbolplayerstokens.modelo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Player Model Tests")
class PlayerTest {

    @Test
    @DisplayName("Debería crear un Player con valores por defecto usando Builder")
    void testCreatePlayerWithBuilder() {
        Player player = Player.builder()
                .name("Lionel Messi")
                .team("Inter Miami")
                .league("MLS")
                .position("RW")
                .rating(8.5)
                .goals(5)
                .assists(3)
                .build();

        assertNotNull(player);
        assertEquals("Lionel Messi", player.getName());
        assertEquals("Inter Miami", player.getTeam());
        assertEquals("MLS", player.getLeague());
        assertEquals("RW", player.getPosition());
        assertEquals(8.5, player.getRating());
        assertEquals(5, player.getGoals());
        assertEquals(3, player.getAssists());

        // Verificar valores por defecto
    }

    @Test
    @DisplayName("Debería establecer lastModifiedAt al crear un nuevo Player")
    void testLastModifiedAtOnCreation() {
        LocalDateTime beforeCreation = LocalDateTime.now();

        Player player = Player.builder()
                .name("Cristiano Ronaldo")
                .team("Al Nassr")
                .league("Saudi Arabia")
                .build();

        player.applyPersistenceDefaults();

        LocalDateTime afterCreation = LocalDateTime.now();

        assertNotNull(player.getLastModifiedAt());
        assertTrue(player.getLastModifiedAt().isAfter(beforeCreation.minusSeconds(1)));
        assertTrue(player.getLastModifiedAt().isBefore(afterCreation.plusSeconds(1)));
    }

    @Test
    @DisplayName("Debería aplicar valores por defecto para campos nulos")
    void testApplyPersistenceDefaults() {
        Player player = Player.builder()
                .name("Test Player")
                .team("Test Team")
                .build();

        player.applyPersistenceDefaults();

        
        assertEquals(0, player.getShotsOnTarget());
        assertEquals(1, player.getClears());
        assertEquals(1, player.getTackles());
        assertEquals(1, player.getInterceptions());
        assertEquals(1, player.getBlocks());
        assertEquals(1, player.getOwnGoals());
        assertEquals(1, player.getKeyPasses());
        
    }

    @Test
    @DisplayName("Debería mantener valores personalizados al aplicar defaults")
    void testApplyPersistenceDefaultsWithCustomValues() {
        Player player = Player.builder()
                .name("Custom Player")
                .team("Custom Team")
            
                .passAccuracy(0.85)
                .build();

        player.applyPersistenceDefaults();

        
        assertEquals(0.85, player.getPassAccuracy());
    }

    @Test
    @DisplayName("Debería crear un Player sin argumentos")
    void testCreatePlayerWithNoArgsConstructor() {
        Player player = new Player();

        assertNotNull(player);
        assertNull(player.getId());
        assertNull(player.getName());
        assertNull(player.getTeam());
    }

    @Test
    @DisplayName("Debería establecer y obtener todos los campos")
    void testSettersAndGetters() {
        Player player = new Player();

        player.setName("Test Player");
        player.setTeam("Test Team");
        player.setLeague("Test League");
        player.setPosition("ST");
        player.setRating(7.5);
        player.setAppearances(10);
        player.setMinutes(900);
        player.setGoals(5);
        player.setAssists(2);
        player.setYellowCards(1);
        player.setRedCards(0);
        player.setPlayerOfTheMatch(2);
        player.setScore(new BigDecimal("75.50"));
        player.setLastModifiedAt(LocalDateTime.now());

        assertEquals("Test Player", player.getName());
        assertEquals("Test Team", player.getTeam());
        assertEquals("Test League", player.getLeague());
        assertEquals("ST", player.getPosition());
        assertEquals(7.5, player.getRating());
        assertEquals(10, player.getAppearances());
        assertEquals(900, player.getMinutes());
        assertEquals(5, player.getGoals());
        assertEquals(2, player.getAssists());
        assertEquals(1, player.getYellowCards());
        assertEquals(0, player.getRedCards());
        assertEquals(2, player.getPlayerOfTheMatch());
        assertEquals(new BigDecimal("75.50"), player.getScore());
        assertNotNull(player.getLastModifiedAt());
    }

    @Test
    @DisplayName("Debería tener valores por defecto al construir con Builder")
    void testBuilderDefaultValues() {
        Player player = Player.builder()
                .name("Default Values Test")
                .team("Test Team")
                .build();

        
        assertEquals(0, player.getShotsOnTarget());
        assertEquals(1.0, player.getPassAccuracy());
    }

    @Test
    @DisplayName("Debería ser comparable por atributos")
    void testPlayerEquality() {
        Player player1 = Player.builder()
                .name("Messi")
                .team("Inter Miami")
                .build();

        Player player2 = Player.builder()
                .name("Messi")
                .team("Inter Miami")
                .build();

        assertEquals(player1.getName(), player2.getName());
        assertEquals(player1.getTeam(), player2.getTeam());
    }

    @Test
    @DisplayName("Debería aceptar valores nulos para campos opcionales")
    void testNullableFields() {
        Player player = Player.builder()
                .name("Nullable Test")
                .team("Test Team")
                .rating(null)
                .goals(null)
                .assists(null)
                .build();

        assertNull(player.getRating());
        assertNull(player.getGoals());
        assertNull(player.getAssists());
    }

    @Test
    @DisplayName("Debería manejar valores Big Decimal para score")
    void testScoreBigDecimal() {
        BigDecimal score = new BigDecimal("85.12345678");
        Player player = Player.builder()
                .name("Score Test")
                .team("Test Team")
                .score(score)
                .build();

        assertEquals(score, player.getScore());
        assertEquals("85.12345678", player.getScore().toPlainString());
    }
}
