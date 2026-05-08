package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerDetailDTO Tests")
class PlayerDetailDTOTest {

    @Test
    @DisplayName("Debería crear un PlayerDetailDTO con Builder")
    void testCreatePlayerDetailDTOWithBuilder() {
        PlayerDetailDTO dto = PlayerDetailDTO.builder()
                .id(1L)
                .name("Messi")
                .team("Inter Miami")
                .league("MLS")
                .position("RW")
                .rating(8.5)
                
                .goals(5)
                .assists(3)
                .passAccuracy(0.91)
                .aerialWon(4.0)
                .faults(1.0)
                .offsidesGiven(2.0)
                .build();

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Messi", dto.getName());
        assertEquals("Inter Miami", dto.getTeam());
        assertEquals("MLS", dto.getLeague());
        assertEquals("RW", dto.getPosition());
        assertEquals(8.5, dto.getRating());
        assertEquals(5, dto.getGoals());
        assertEquals(3, dto.getAssists());
        assertEquals(0.91, dto.getPassAccuracy());
        assertEquals(4, dto.getAerialWon());
        assertEquals(1, dto.getFaults());
        assertEquals(2, dto.getOffsidesGiven());
    }

    @Test
    @DisplayName("Debería convertir un Player a PlayerDetailDTO")
    void testToDTOFromPlayer() {
        Player player = Player.builder()
                .id(1L)
                .name("Lionel Messi")
                .team("Inter Miami")
                .league("MLS")
                .position("RW")
                .rating(8.5)
                .appearances(10)
                .appearances(10)
                .minutes(900)
                .goals(5)
                .assists(3)
                .shotsOnTarget(15.0)
                .passAccuracy(0.92)
                .aerialWon(4.0)
                .faults(1.0)
                .offsidesGiven(2.0)
                .clears(2.0)
                .dribbled(3.0)
                .tackles(5.0)
                .interceptions(3.0)
                .blocks(1.0)
                .ownGoals(0)
                .keyPasses(10.0)
                .dribbles(8.0)
                .faulted(2.0)
                .offsides(1.0)
                .dispossesed(4.0)
                .turnover(6.0)
                .passAccuracy(0.92)
                .yellowCards(0)
                .redCards(0)
                .playerOfTheMatch(2)
                .score(new BigDecimal("85.50"))
                .lastModifiedAt(LocalDateTime.now())
                .build();

        PlayerDetailDTO dto = PlayerDetailDTO.toDTO(player);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Lionel Messi", dto.getName());
        assertEquals("Inter Miami", dto.getTeam());
        assertEquals("MLS", dto.getLeague());
        assertEquals("RW", dto.getPosition());
        assertEquals(8.5, dto.getRating());
        assertEquals(10, dto.getAppearances());
        assertEquals(10, dto.getAppearances());
        assertEquals(900, dto.getMinutes());
        assertEquals(5, dto.getGoals());
        assertEquals(3, dto.getAssists());
        assertEquals(15, dto.getShotsOnTarget());
        assertEquals(0.92, dto.getPassAccuracy());
        assertEquals(4, dto.getAerialWon());
        assertEquals(1, dto.getFaults());
        assertEquals(2, dto.getOffsidesGiven());
        assertEquals(2, dto.getClears());
        assertEquals(3, dto.getDribbled());
        assertEquals(5, dto.getTackles());
        assertEquals(3, dto.getInterceptions());
        assertEquals(1, dto.getBlocks());
        assertEquals(0, dto.getOwnGoals());
        assertEquals(10, dto.getKeyPasses());
        assertEquals(8, dto.getDribbles());
        assertEquals(2, dto.getFaulted());
        assertEquals(1, dto.getOffsides());
        assertEquals(4, dto.getDispossesed());
        assertEquals(6, dto.getTurnover());
        assertEquals(0.92, dto.getPassAccuracy());
        assertEquals(0, dto.getYellowCards());
        assertEquals(0, dto.getRedCards());
        assertEquals(2, dto.getPlayerOfTheMatch());
        assertEquals(new BigDecimal("85.50"), dto.getScore());
    }

    @Test
    @DisplayName("Debería manejar valores nulos al convertir Player a DTO")
    void testToDTOWithNullValues() {
        Player player = Player.builder()
                .id(1L)
                .name("Test Player")
                .team("Test Team")
                .build();

        PlayerDetailDTO dto = PlayerDetailDTO.toDTO(player);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Test Player", dto.getName());
        assertEquals("Test Team", dto.getTeam());
        assertNull(dto.getRating());
        assertNull(dto.getGoals());
        assertNull(dto.getAssists());
    }

    @Test
    @DisplayName("Debería establecer y obtener todos los campos")
    void testSettersAndGetters() {
        PlayerDetailDTO dto = PlayerDetailDTO.builder()
                .id(1L)
                .name("Test Player")
                .team("Test Team")
                .league("Test League")
                .position("ST")
                .rating(7.5)
                
                .appearances(5)
                .minutes(450)
                .goals(3)
                .assists(2)
                .shotsOnTarget(10.0)
                .clears(5.0)
                .tackles(8.0)
                .interceptions(4.0)
                .blocks(2.0)
                .ownGoals(0)
                .keyPasses(6.0)
                .passAccuracy(0.88)
                .yellowCards(1)
                .redCards(0)
                .playerOfTheMatch(1)
                .score(new BigDecimal("72.50"))
                .build();

        assertEquals(1L, dto.getId());
        assertEquals("Test Player", dto.getName());
        assertEquals("Test Team", dto.getTeam());
        assertEquals("Test League", dto.getLeague());
        assertEquals("ST", dto.getPosition());
        assertEquals(7.5, dto.getRating());
        assertEquals(5, dto.getAppearances());
        assertEquals(450, dto.getMinutes());
        assertEquals(3, dto.getGoals());
        assertEquals(2, dto.getAssists());
        assertEquals(10, dto.getShotsOnTarget());
        assertEquals(5, dto.getClears());
        assertEquals(8, dto.getTackles());
        assertEquals(4, dto.getInterceptions());
        assertEquals(2, dto.getBlocks());
        assertEquals(0, dto.getOwnGoals());
        assertEquals(6, dto.getKeyPasses());
        assertEquals(0.88, dto.getPassAccuracy());
        assertEquals(1, dto.getYellowCards());
        assertEquals(0, dto.getRedCards());
        assertEquals(1, dto.getPlayerOfTheMatch());
        assertEquals(new BigDecimal("72.50"), dto.getScore());
    }

    @Test
    @DisplayName("Debería manejar BigDecimal para score")
    void testScoreBigDecimal() {
        BigDecimal score = new BigDecimal("85.12345678");
        PlayerDetailDTO dto = PlayerDetailDTO.builder()
                .name("Score Test")
                .team("Test Team")
                .score(score)
                .build();

        assertEquals(score, dto.getScore());
        assertEquals("85.12345678", dto.getScore().toPlainString());
    }

    @Test
    @DisplayName("Debería comparar DTOs por contenido")
    void testDTOEquality() {
        PlayerDetailDTO dto1 = PlayerDetailDTO.builder()
                .name("Messi")
                .team("Inter Miami")
                .league("MLS")
                .rating(8.5)
                .build();

        PlayerDetailDTO dto2 = PlayerDetailDTO.builder()
                .name("Messi")
                .team("Inter Miami")
                .league("MLS")
                .rating(8.5)
                .build();

        assertEquals(dto1, dto2);
    }

    @Test
    @DisplayName("Debería manejar nulos en comparación")
    void testDTOWithNulls() {
        PlayerDetailDTO dto = PlayerDetailDTO.builder()
                .name("Test Player")
                .build();

        assertEquals("Test Player", dto.getName());
        assertNull(dto.getId());
        assertNull(dto.getTeam());
        assertNull(dto.getRating());
    }

    @Test
    @DisplayName("Debería generar hash code consistente")
    void testHashCode() {
        PlayerDetailDTO dto1 = PlayerDetailDTO.builder()
                .name("Messi")
                .team("Inter Miami")
                .build();

        PlayerDetailDTO dto2 = PlayerDetailDTO.builder()
                .name("Messi")
                .team("Inter Miami")
                .build();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("Debería convertir Player con todos los campos a DTO completo")
    void testCompletePlayerToDTOConversion() {
        Player player = Player.builder()
                .id(5L)
                .name("Cristiano Ronaldo")
                .team("Al Nassr")
                .league("Saudi Arabia")
                .position("ST")
                .rating(7.8)
                
                .appearances(15)
                .minutes(1350)
                .goals(12)
                .assists(4)
                .shotsOnTarget(30.0)
                .clears(5.0)
                .tackles(10.0)
                .interceptions(5.0)
                .blocks(3.0)
                .ownGoals(0)
                .keyPasses(15.0)
                .passAccuracy(0.85)
                .yellowCards(2)
                .redCards(0)
                .playerOfTheMatch(3)
                .score(new BigDecimal("78.50"))
                .lastModifiedAt(LocalDateTime.now())
                .build();

        PlayerDetailDTO dto = PlayerDetailDTO.toDTO(player);

        assertEquals(player.getId(), dto.getId());
        assertEquals(player.getName(), dto.getName());
        assertEquals(player.getTeam(), dto.getTeam());
        assertEquals(player.getLeague(), dto.getLeague());
        assertEquals(player.getPosition(), dto.getPosition());
        assertEquals(player.getRating(), dto.getRating());
        assertEquals(player.getAppearances(), dto.getAppearances());
        assertEquals(player.getAppearances(), dto.getAppearances());
        assertEquals(player.getMinutes(), dto.getMinutes());
        assertEquals(player.getGoals(), dto.getGoals());
        assertEquals(player.getAssists(), dto.getAssists());
        assertEquals(player.getShotsOnTarget(), dto.getShotsOnTarget());
        assertEquals(player.getClears(), dto.getClears());
        assertEquals(player.getTackles(), dto.getTackles());
        assertEquals(player.getInterceptions(), dto.getInterceptions());
        assertEquals(player.getBlocks(), dto.getBlocks());
        assertEquals(player.getOwnGoals(), dto.getOwnGoals());
        assertEquals(player.getKeyPasses(), dto.getKeyPasses());
        assertEquals(player.getPassAccuracy(), dto.getPassAccuracy());
        assertEquals(player.getYellowCards(), dto.getYellowCards());
        assertEquals(player.getRedCards(), dto.getRedCards());
        assertEquals(player.getPlayerOfTheMatch(), dto.getPlayerOfTheMatch());
        assertEquals(player.getScore(), dto.getScore());
    }
}
