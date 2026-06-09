package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerDTO Tests")
class PlayerDTOTest {

    @Test
    @DisplayName("Debería crear un PlayerDTO con Builder")
    void testCreatePlayerDTOWithBuilder() {
        PlayerDTO dto = PlayerDTO.builder()
                .id(1L)
                .name("Cristiano Ronaldo")
                .team("Al Nassr")
                .league("Saudi League")
                .position("Forward")
                .score(new BigDecimal("85.50"))
                .altPosition("Midfielder")
                .build();

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Cristiano Ronaldo", dto.getName());
        assertEquals("Al Nassr", dto.getTeam());
        assertEquals("Saudi League", dto.getLeague());
        assertEquals("Forward", dto.getPosition());
        assertEquals(new BigDecimal("85.50"), dto.getScore());
        assertEquals("Midfielder", dto.getAltPosition());
    }

    @Test
    @DisplayName("Debería convertir un Player a PlayerDTO")
    void testToDTOFromPlayer() {
        Player player = Player.builder()
                .id(1L)
                .name("Lionel Messi")
                .team("Inter Miami")
                .league("MLS")
                .position("RW")
                .score(new BigDecimal("85.50"))
                .altPosition("Midfielder")
                .build();

        PlayerDTO dto = PlayerDTO.toDTO(player);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Lionel Messi", dto.getName());
        assertEquals("Inter Miami", dto.getTeam());
        assertEquals("MLS", dto.getLeague());
        assertEquals("RW", dto.getPosition());
        assertEquals(new BigDecimal("85.50"), dto.getScore());
        assertEquals("Midfielder", dto.getAltPosition());
    }

    @Test
    @DisplayName("Debería manejar valores nulos al convertir Player a DTO")
    void testToDTOWithNullValues() {
        Player player = Player.builder()
                .id(1L)
                .name("Test Player")
                .team("Test Team")
                .build();

        PlayerDTO dto = PlayerDTO.toDTO(player);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Test Player", dto.getName());
        assertEquals("Test Team", dto.getTeam());
        assertNull(dto.getLeague());
        assertNull(dto.getPosition());
        assertNull(dto.getScore());
        assertNull(dto.getAltPosition());
    }

    @Test
    @DisplayName("Debería establecer y obtener todos los campos")
    void testSettersAndGetters() {
        PlayerDTO dto = PlayerDTO.builder()
                .id(1L)
                .name("Test Player")
                .team("Test Team")
                .league("Test League")
                .position("ST")
                .score(new BigDecimal("72.50"))
                .altPosition("CAM")
                .build();

        assertEquals(1L, dto.getId());
        assertEquals("Test Player", dto.getName());
        assertEquals("Test Team", dto.getTeam());
        assertEquals("Test League", dto.getLeague());
        assertEquals("ST", dto.getPosition());
        assertEquals(new BigDecimal("72.50"), dto.getScore());
        assertEquals("CAM", dto.getAltPosition());
    }

    @Test
    @DisplayName("Debería manejar BigDecimal para score")
    void testScoreBigDecimal() {
        BigDecimal score = new BigDecimal("85.12345678");
        PlayerDTO dto = PlayerDTO.builder()
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
        PlayerDTO dto1 = PlayerDTO.builder()
                .name("Messi")
                .team("Inter Miami")
                .league("MLS")
                .position("RW")
                .score(new BigDecimal("85.50"))
                .build();

        PlayerDTO dto2 = PlayerDTO.builder()
                .name("Messi")
                .team("Inter Miami")
                .league("MLS")
                .position("RW")
                .score(new BigDecimal("85.50"))
                .build();

        assertEquals(dto1, dto2);
    }

    @Test
    @DisplayName("Debería manejar nulos en comparación")
    void testDTOWithNulls() {
        PlayerDTO dto = PlayerDTO.builder()
                .name("Test Player")
                .build();

        assertEquals("Test Player", dto.getName());
        assertNull(dto.getId());
        assertNull(dto.getTeam());
        assertNull(dto.getLeague());
        assertNull(dto.getPosition());
        assertNull(dto.getScore());
        assertNull(dto.getAltPosition());
    }

    @Test
    @DisplayName("Debería generar hash code consistente")
    void testHashCode() {
        PlayerDTO dto1 = PlayerDTO.builder()
                .name("Messi")
                .team("Inter Miami")
                .build();

        PlayerDTO dto2 = PlayerDTO.builder()
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
                .league("Saudi League")
                .position("ST")
                .score(new BigDecimal("78.50"))
                .altPosition("LW")
                .build();

        PlayerDTO dto = PlayerDTO.toDTO(player);

        assertEquals(player.getId(), dto.getId());
        assertEquals(player.getName(), dto.getName());
        assertEquals(player.getTeam(), dto.getTeam());
        assertEquals(player.getLeague(), dto.getLeague());
        assertEquals(player.getPosition(), dto.getPosition());
        assertEquals(player.getScore(), dto.getScore());
        assertEquals(player.getAltPosition(), dto.getAltPosition());
    }
}
