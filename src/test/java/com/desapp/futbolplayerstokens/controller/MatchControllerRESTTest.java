package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.MatchDTO;
import com.desapp.futbolplayerstokens.modelo.Match;
import com.desapp.futbolplayerstokens.service.MatchScraperService;
import com.desapp.futbolplayerstokens.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchControllerRESTTest {

    @Mock
    private MatchService matchService;

    @Mock
    private MatchScraperService matchScraperService;

    @InjectMocks
    private MatchControllerREST matchController;

    private Match match1;
    private Match match2;
    private Match match3;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        match1 = Match.builder()
                .id(1L)
                .footballDataMatchId(100L)
                .team1Id(1L)
                .team2Id(2L)
                .matchTime(now)
                .build();

        match2 = Match.builder()
                .id(2L)
                .footballDataMatchId(101L)
                .team1Id(1L)
                .team2Id(3L)
                .matchTime(now.plusHours(2))
                .build();

        match3 = Match.builder()
                .id(3L)
                .footballDataMatchId(102L)
                .team1Id(4L)
                .team2Id(5L)
                .matchTime(now.plusHours(4))
                .build();
    }

    @Test
    void testGetAllMatches() {
        when(matchService.getAllMatches()).thenReturn(List.of(match1, match2, match3));

        ResponseEntity<List<MatchDTO>> response = matchController.getAllMatches();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        verify(matchService, times(1)).getAllMatches();
    }

    @Test
    void testGetAllMatches_Empty() {
        when(matchService.getAllMatches()).thenReturn(List.of());

        ResponseEntity<List<MatchDTO>> response = matchController.getAllMatches();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(matchService, times(1)).getAllMatches();
    }

    @Test
    void testGetMatchById_Found() {
        when(matchService.getMatchById(1L)).thenReturn(Optional.of(match1));

        ResponseEntity<MatchDTO> response = matchController.getMatchById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(100L, response.getBody().getFootballDataMatchId());
        verify(matchService, times(1)).getMatchById(1L);
    }

    @Test
    void testGetMatchById_NotFound() {
        when(matchService.getMatchById(999L)).thenReturn(Optional.empty());

        ResponseEntity<MatchDTO> response = matchController.getMatchById(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(matchService, times(1)).getMatchById(999L);
    }

    @Test
    void testGetMatchesByTeamId() {
        when(matchService.getMatchesByTeamId(1L)).thenReturn(List.of(match1, match2));

        ResponseEntity<List<MatchDTO>> response = matchController.getMatchesByTeamId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(matchService, times(1)).getMatchesByTeamId(1L);
    }

    @Test
    void testGetMatchesByTeamId_NoResults() {
        when(matchService.getMatchesByTeamId(999L)).thenReturn(List.of());

        ResponseEntity<List<MatchDTO>> response = matchController.getMatchesByTeamId(999L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(matchService, times(1)).getMatchesByTeamId(999L);
    }

    @Test
    void testCreateMatch() {
        MatchDTO createDTO = MatchDTO.builder()
                .footballDataMatchId(100L)
                .team1Id(1L)
                .team2Id(2L)
                .matchTime(LocalDateTime.now())
                .build();

        when(matchService.createMatch(any(Match.class))).thenReturn(match1);

        ResponseEntity<MatchDTO> response = matchController.createMatch(createDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(100L, response.getBody().getFootballDataMatchId());
        verify(matchService, times(1)).createMatch(any(Match.class));
    }

    @Test
    void testCreateMatch_WithAllFields() {
        LocalDateTime matchTime = LocalDateTime.now();
        MatchDTO createDTO = MatchDTO.builder()
                .footballDataMatchId(200L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(matchTime)
                .build();

        Match expectedMatch = Match.builder()
                .id(5L)
                .footballDataMatchId(200L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(matchTime)
                .build();

        when(matchService.createMatch(any(Match.class))).thenReturn(expectedMatch);

        ResponseEntity<MatchDTO> response = matchController.createMatch(createDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().getId());
        assertEquals(200L, response.getBody().getFootballDataMatchId());
        assertEquals(10L, response.getBody().getTeam1Id());
        assertEquals(20L, response.getBody().getTeam2Id());
        verify(matchService, times(1)).createMatch(any(Match.class));
    }

    @Test
    void testUpdateMatch() {
        MatchDTO updateDTO = MatchDTO.builder()
                .footballDataMatchId(150L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(LocalDateTime.now().plusHours(1))
                .build();

        Match updatedMatch = Match.builder()
                .id(1L)
                .footballDataMatchId(150L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(LocalDateTime.now().plusHours(1))
                .build();

        when(matchService.updateMatch(anyLong(), any(Match.class))).thenReturn(updatedMatch);

        ResponseEntity<MatchDTO> response = matchController.updateMatch(1L, updateDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(150L, response.getBody().getFootballDataMatchId());
        assertEquals(10L, response.getBody().getTeam1Id());
        verify(matchService, times(1)).updateMatch(anyLong(), any(Match.class));
    }

    @Test
    void testUpdateMatch_NotFound() {
        MatchDTO updateDTO = MatchDTO.builder()
                .footballDataMatchId(150L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(LocalDateTime.now())
                .build();

        when(matchService.updateMatch(anyLong(), any(Match.class)))
                .thenThrow(new RuntimeException("Match not found with id: 999"));

        assertThrows(RuntimeException.class, () -> matchController.updateMatch(999L, updateDTO));
        verify(matchService, times(1)).updateMatch(anyLong(), any(Match.class));
    }

    @Test
    void testDeleteMatch() {
        doNothing().when(matchService).deleteMatch(1L);

        ResponseEntity<Void> response = matchController.deleteMatch(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(matchService, times(1)).deleteMatch(1L);
    }

    @Test
    void testDeleteMatch_NotFound() {
        doThrow(new RuntimeException("Match not found with id: 999"))
                .when(matchService).deleteMatch(999L);

        assertThrows(RuntimeException.class, () -> matchController.deleteMatch(999L));
        verify(matchService, times(1)).deleteMatch(999L);
    }

    @Test
    void testScrapeMatchesOfToday() {
        when(matchScraperService.scrapeMatchesOfToday()).thenReturn(List.of(match1, match2));

        ResponseEntity<List<MatchDTO>> response = matchController.scrapeMatchesOfToday();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(matchScraperService, times(1)).scrapeMatchesOfToday();
    }

    @Test
    void testScrapeMatchesOfToday_Empty() {
        when(matchScraperService.scrapeMatchesOfToday()).thenReturn(List.of());

        ResponseEntity<List<MatchDTO>> response = matchController.scrapeMatchesOfToday();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(matchScraperService, times(1)).scrapeMatchesOfToday();
    }

    @Test
    void testGetMatchById_VerifyDTO() {
        when(matchService.getMatchById(1L)).thenReturn(Optional.of(match1));

        ResponseEntity<MatchDTO> response = matchController.getMatchById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(100L, response.getBody().getFootballDataMatchId());
        assertEquals(1L, response.getBody().getTeam1Id());
        assertEquals(2L, response.getBody().getTeam2Id());
        assertNotNull(response.getBody().getMatchTime());
        verify(matchService, times(1)).getMatchById(1L);
    }

    @Test
    void testCreateMatch_DTOConversion() {
        MatchDTO createDTO = MatchDTO.builder()
                .footballDataMatchId(300L)
                .team1Id(5L)
                .team2Id(6L)
                .matchTime(LocalDateTime.now())
                .build();

        Match savedMatch = Match.builder()
                .id(10L)
                .footballDataMatchId(300L)
                .team1Id(5L)
                .team2Id(6L)
                .matchTime(LocalDateTime.now())
                .build();

        when(matchService.createMatch(any(Match.class))).thenReturn(savedMatch);

        ResponseEntity<MatchDTO> response = matchController.createMatch(createDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(300L, response.getBody().getFootballDataMatchId());
        verify(matchService, times(1)).createMatch(any(Match.class));
    }
}
