package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDetailDTO;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.service.PlayerOverwriteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService Tests")
class PlayerServiceImplTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerServiceImpl playerService;

    private PlayerDetailDTO playerDetailDTO;
    private Player player;

    @BeforeEach
    void setUp() {
        playerDetailDTO = PlayerDetailDTO.builder()
                .name("Lionel Messi")
                .team("Inter Miami")
                .league("MLS")
                .position("RW")
                .rating(8.5)
                .appearances(10)
                .minutes(900)
                .goals(5)
                .assists(3)
                .yellowCards(0)
                .redCards(0)
                .playerOfTheMatch(2)
                .build();

        player = Player.builder()
                .id(1L)
                .name("Lionel Messi")
                .team("Inter Miami")
                .league("MLS")
                .position("RW")
                .rating(8.5)
                .appearances(10)
                .minutes(900)
                .goals(5)
                .assists(3)
                .yellowCards(0)
                .redCards(0)
                .playerOfTheMatch(2)
                .lastModifiedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Debería obtener un jugador por ID")
    void testGetPlayerById() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        PlayerDetailDTO result = playerService.getPlayerById(1L);

        assertNotNull(result);
        assertEquals("Lionel Messi", result.getName());
        assertEquals("Inter Miami", result.getTeam());
        assertEquals(8.5, result.getRating());
        verify(playerRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando no encuentra el jugador por ID")
    void testGetPlayerByIdNotFound() {
        when(playerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> playerService.getPlayerById(999L));
        verify(playerRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Debería guardar todos los jugadores nuevos")
    void testSaveAllPlayers() {
        List<PlayerDetailDTO> players = new ArrayList<>();
        players.add(playerDetailDTO);

        when(playerRepository.findAll()).thenReturn(new ArrayList<>());
        when(playerRepository.save(any(Player.class))).thenReturn(player);

        playerService.saveAllPlayers(players);

        verify(playerRepository, times(1)).save(any(Player.class));
    }

    @Test
    @DisplayName("Debería ignorar jugadores duplicados al guardar")
    void testSaveAllPlayersIgnoreDuplicates() {
        List<PlayerDetailDTO> players = new ArrayList<>();
        players.add(playerDetailDTO);

        when(playerRepository.findAll()).thenReturn(List.of(player));

        playerService.saveAllPlayers(players);

        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    @DisplayName("Debería retornar true si el jugador existe")
    void testPlayerExists() {
        when(playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase("Messi", "Inter Miami"))
                .thenReturn(List.of(player));

        boolean exists = playerService.playerExists("Messi", "Inter Miami");

        assertTrue(exists);
        verify(playerRepository, times(1)).findByNameIgnoreCaseAndTeamIgnoreCase(anyString(), anyString());
    }

    @Test
    @DisplayName("Debería retornar false si el jugador no existe")
    void testPlayerNotExists() {
        when(playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase("Messi", "Barcelona"))
                .thenReturn(new ArrayList<>());

        boolean exists = playerService.playerExists("Messi", "Barcelona");

        assertFalse(exists);
        verify(playerRepository, times(1)).findByNameIgnoreCaseAndTeamIgnoreCase(anyString(), anyString());
    }

    @Test
    @DisplayName("Debería insertar nuevos jugadores con overwrite")
    void testOverwritePlayersByNameAndTeamInsert() {
        List<PlayerDetailDTO> players = new ArrayList<>();
        players.add(playerDetailDTO);

        when(playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(anyString(), anyString()))
                .thenReturn(new ArrayList<>());
        when(playerRepository.save(any(Player.class))).thenReturn(player);

        PlayerOverwriteResult result = playerService.overwritePlayersByNameAndTeam(players);

        assertNotNull(result);
        assertEquals(1, result.getInsertedRows());
        assertEquals(0, result.getModifiedRows());
        assertEquals(1, result.getRosterPlayersFound());
        verify(playerRepository, times(1)).save(any(Player.class));
    }

    @Test
    @DisplayName("Debería actualizar jugadores existentes con overwrite")
    void testOverwritePlayersByNameAndTeamUpdate() {
        List<PlayerDetailDTO> players = new ArrayList<>();
        playerDetailDTO.setRating(9.0);
        players.add(playerDetailDTO);

        Player existingPlayer = Player.builder()
                .id(1L)
                .name("Lionel Messi")
                .team("Inter Miami")
                .league("MLS")
                .rating(8.0)
                .goals(4)
                .build();

        when(playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of(existingPlayer));
        when(playerRepository.saveAll(any())).thenReturn(List.of(existingPlayer));

        PlayerOverwriteResult result = playerService.overwritePlayersByNameAndTeam(players);

        assertNotNull(result);
        assertEquals(0, result.getInsertedRows());
        assertEquals(1, result.getModifiedRows());
        verify(playerRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Debería mezclar inserciones y actualizaciones")
    void testOverwritePlayersMixed() {
        PlayerDetailDTO newPlayer = PlayerDetailDTO.builder()
                .name("New Player")
                .team("New Team")
                .league("MLS")
                .rating(7.0)
                .build();

        PlayerDetailDTO existingPlayer = PlayerDetailDTO.builder()
                .name("Lionel Messi")
                .team("Inter Miami")
                .league("MLS")
                .rating(9.0)
                .build();

        List<PlayerDetailDTO> players = List.of(newPlayer, existingPlayer);

        Player existingPlayerEntity = Player.builder()
                .id(1L)
                .name("Lionel Messi")
                .team("Inter Miami")
                .build();

        when(playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase("New Player", "New Team"))
                .thenReturn(new ArrayList<>());
        when(playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase("Lionel Messi", "Inter Miami"))
                .thenReturn(List.of(existingPlayerEntity));

        when(playerRepository.save(any(Player.class))).thenReturn(player);
        when(playerRepository.saveAll(any())).thenReturn(List.of(existingPlayerEntity));

        PlayerOverwriteResult result = playerService.overwritePlayersByNameAndTeam(players);

        assertNotNull(result);
        assertEquals(1, result.getInsertedRows());
        assertEquals(1, result.getModifiedRows());
        assertEquals(2, result.getRosterPlayersFound());
    }

    @Test
    @DisplayName("Debería ignorar jugadores con nombre o equipo en blanco")
    void testOverwritePlayersIgnoreBlank() {
        PlayerDetailDTO blankPlayer = PlayerDetailDTO.builder()
                .name("  ")
                .team("Inter Miami")
                .league("MLS")
                .build();

        PlayerDetailDTO validPlayer = PlayerDetailDTO.builder()
                .name("Messi")
                .team("Inter Miami")
                .league("MLS")
                .build();

        List<PlayerDetailDTO> players = List.of(blankPlayer, validPlayer);

        when(playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(anyString(), anyString()))
                .thenReturn(new ArrayList<>());
        when(playerRepository.save(any(Player.class))).thenReturn(player);

        PlayerOverwriteResult result = playerService.overwritePlayersByNameAndTeam(players);

        assertEquals(1, result.getInsertedRows());
        verify(playerRepository, times(1)).save(any(Player.class));
    }

    @Test
    @DisplayName("Debería actualizar todos los campos al modificar un jugador")
    void testUpdatePlayerFields() {
        Player existingPlayer = Player.builder()
                .id(1L)
                .name("Messi")
                .team("Inter Miami")
                .rating(8.0)
                .goals(4)
                .assists(2)
                .appearances(5)
                .minutes(450)
                .yellowCards(0)
                .redCards(0)
                .playerOfTheMatch(0)
                .build();

        PlayerDetailDTO updatedDTO = PlayerDetailDTO.builder()
                .name("Messi")
                .team("Inter Miami")
                .rating(9.5)
                .goals(10)
                .assists(5)
                .appearances(15)
                .minutes(1350)
                .yellowCards(1)
                .redCards(0)
                .playerOfTheMatch(3)
                .build();

        when(playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(anyString(), anyString()))
                .thenReturn(List.of(existingPlayer));
        when(playerRepository.saveAll(any())).thenReturn(List.of(existingPlayer));

        PlayerOverwriteResult result = playerService.overwritePlayersByNameAndTeam(List.of(updatedDTO));

        assertEquals(1, result.getModifiedRows());
        verify(playerRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Debería manejar lista vacía de jugadores")
    void testSaveAllPlayersEmpty() {
        List<PlayerDetailDTO> players = new ArrayList<>();

        playerService.saveAllPlayers(players);

        verify(playerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería manejar overwrite con lista vacía")
    void testOverwritePlayersEmpty() {
        List<PlayerDetailDTO> players = new ArrayList<>();

        PlayerOverwriteResult result = playerService.overwritePlayersByNameAndTeam(players);

        assertNotNull(result);
        assertEquals(0, result.getInsertedRows());
        assertEquals(0, result.getModifiedRows());
        assertEquals(0, result.getRosterPlayersFound());
    }

    @Test
    @DisplayName("Debería trimear espacios en búsquedas")
    void testPlayerExistsTrimmed() {
        when(playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase("Messi", "Inter Miami"))
                .thenReturn(List.of(player));

        boolean exists = playerService.playerExists("  Messi  ", "  Inter Miami  ");

        assertTrue(exists);
    }
}
