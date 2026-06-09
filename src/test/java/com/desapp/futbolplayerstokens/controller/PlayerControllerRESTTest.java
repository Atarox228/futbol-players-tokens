package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;
import com.desapp.futbolplayerstokens.controller.dto.PlayerDetailDTO;
import com.desapp.futbolplayerstokens.controller.dto.PlayerRankingDTO;
import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.modelo.TeamEnum;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import com.desapp.futbolplayerstokens.service.PlayerService;
import com.desapp.futbolplayerstokens.service.QuoteService;
import com.desapp.futbolplayerstokens.service.RankingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerControllerRESTTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private PlayerScraperService scraperService;

    @Mock
    private QuoteService quoteService;

    @Mock
    private RankingService rankingService;

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerControllerREST playerController;

    @Test
    void hello_returnsHelloWorld() {
        assertEquals("Hello World", playerController.hello());
    }

    @Test
    void getPlayers_withFilters_returnsList() {
        when(playerService.getPlayersWithFilters("LaLiga", "Barcelona", "Forward"))
                .thenReturn(List.of(PlayerDTO.builder().id(1L).name("Messi").build()));

        ResponseEntity<List<PlayerDTO>> result = playerController.getPlayers("LaLiga", "Barcelona", "Forward");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getPlayers_withoutFilters_returnsList() {
        when(playerService.getPlayersWithFilters(null, null, null))
                .thenReturn(List.of());

        ResponseEntity<List<PlayerDTO>> result = playerController.getPlayers(null, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    void getPlayers_serviceError_returnsBadRequest() {
        when(playerService.getPlayersWithFilters(any(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        ResponseEntity<List<PlayerDTO>> result = playerController.getPlayers("x", "y", "z");

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void getRanking_returnsList() {
        when(rankingService.getRanking(0, 20)).thenReturn(List.of(PlayerRankingDTO.builder().playerId("1").rank(1).build()));

        ResponseEntity<List<PlayerRankingDTO>> result = playerController.getRanking(0, 20);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getRanking_serviceError_returnsBadRequest() {
        when(rankingService.getRanking(0, 20)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<List<PlayerRankingDTO>> result = playerController.getRanking(0, 20);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void getPlayer_found() {
        when(playerService.getPlayerById(1L)).thenReturn(PlayerDetailDTO.builder().id(1L).name("Messi").build());

        ResponseEntity<PlayerDetailDTO> result = playerController.getPlayer(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("Messi", result.getBody().getName());
    }

    @Test
    void getPlayer_notFound_returns404() {
        when(playerService.getPlayerById(999L)).thenThrow(new RuntimeException("Player not found"));

        ResponseEntity<PlayerDetailDTO> result = playerController.getPlayer(999L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void getQuotes_returnsList() {
        when(quoteService.getQuotesByPlayerId(1L)).thenReturn(List.of(QuoteDTO.builder().id(1L).playerId(1L).build()));

        ResponseEntity<List<QuoteDTO>> result = playerController.getQuotes(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getQuotes_serviceError_returnsBadRequest() {
        when(quoteService.getQuotesByPlayerId(999L)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<List<QuoteDTO>> result = playerController.getQuotes(999L);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void scrapeAndSavePlayers_triggersScrapeAndRecalculate() {
        doNothing().when(scraperService).scrapeAllPlayersForce();
        doNothing().when(quoteService).recalculateAll(QuoteTrigger.MANUAL);

        ResponseEntity<String> result = playerController.scrapeAndSavePlayers();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(scraperService).scrapeAllPlayersForce();
        verify(quoteService).recalculateAll(QuoteTrigger.MANUAL);
    }

    @Test
    void scrapeAndSavePlayers_error_returns500() {
        doThrow(new RuntimeException("Scrape failed")).when(scraperService).scrapeAllPlayersForce();

        ResponseEntity<String> result = playerController.scrapeAndSavePlayers();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void scrapeAndOverwriteTeamPlayers_success() {
        TeamEnum teamEnum = TeamEnum.BARCELONA;
        String teamName = teamEnum.getName();
        String league = teamEnum.getLeague();

        when(scraperService.scrapeTeamPlayersByName(teamName, league)).thenReturn(List.of());
        Player p1 = Player.builder().id(1L).name("Player1").build();
        when(playerRepository.findByTeamIgnoreCase(teamName)).thenReturn(List.of(p1));

        ResponseEntity<String> result = playerController.scrapeAndOverwriteTeamPlayers(teamEnum.getId());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(scraperService).scrapeTeamPlayersByName(teamName, league);
        verify(playerRepository).findByTeamIgnoreCase(teamName);
        verify(quoteService).recalculatePlayers(List.of(1L), QuoteTrigger.MANUAL);
    }

    @Test
    void scrapeAndOverwriteTeamPlayers_invalidId_returnsError() {
        ResponseEntity<String> result = playerController.scrapeAndOverwriteTeamPlayers(9999);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void scrapeAndOverwriteTeamPlayers_recalculatesAfterScrape() {
        TeamEnum teamEnum = TeamEnum.BARCELONA;
        String teamName = teamEnum.getName();
        String league = teamEnum.getLeague();

        when(scraperService.scrapeTeamPlayersByName(teamName, league)).thenReturn(List.of());
        Player p1 = Player.builder().id(1L).name("Player1").build();
        Player p2 = Player.builder().id(2L).name("Player2").build();
        when(playerRepository.findByTeamIgnoreCase(teamName)).thenReturn(List.of(p1, p2));

        playerController.scrapeAndOverwriteTeamPlayers(teamEnum.getId());

        verify(quoteService).recalculatePlayers(List.of(1L, 2L), QuoteTrigger.MANUAL);
    }

    @Test
    void scrapeAllTeamsByLeague_success() {
        String league = "LaLiga";

        when(scraperService.scrapeLeaguePlayersByStarterTeam("Athletic Club", league)).thenReturn(List.of());
        Player p1 = Player.builder().id(5L).build();
        when(playerRepository.findByFilters(league, null, null)).thenReturn(List.of(p1));

        ResponseEntity<String> result = playerController.scrapeAllTeamsByLeague(league);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(quoteService).recalculatePlayers(List.of(5L), QuoteTrigger.MANUAL);
    }

    @Test
    void scrapeAllTeamsByLeague_invalidLeague_returnsError() {
        ResponseEntity<String> result = playerController.scrapeAllTeamsByLeague("InvalidLeague");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }
}
