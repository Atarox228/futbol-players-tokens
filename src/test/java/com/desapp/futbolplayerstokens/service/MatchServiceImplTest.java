package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.modelo.Match;
import com.desapp.futbolplayerstokens.repository.MatchRepository;
import com.desapp.futbolplayerstokens.service.impl.MatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchServiceImpl matchService;

    private Match match1;
    private Match match2;
    private Match match3;

    @BeforeEach
    void setUp() {
        match1 = Match.builder()
                .id(1L)
                .footballDataMatchId(100L)
                .team1Id(1L)
                .team2Id(2L)
                .matchTime(LocalDateTime.now())
                .build();

        match2 = Match.builder()
                .id(2L)
                .footballDataMatchId(101L)
                .team1Id(1L)
                .team2Id(3L)
                .matchTime(LocalDateTime.now().plusHours(2))
                .build();

        match3 = Match.builder()
                .id(3L)
                .footballDataMatchId(102L)
                .team1Id(4L)
                .team2Id(5L)
                .matchTime(LocalDateTime.now().plusHours(4))
                .build();
    }

    @Test
    void testCreateMatch() {
        when(matchRepository.save(any(Match.class))).thenReturn(match1);

        Match created = matchService.createMatch(match1);

        assertNotNull(created);
        assertEquals(1L, created.getId());
        assertEquals(100L, created.getFootballDataMatchId());
        verify(matchRepository, times(1)).save(match1);
    }

    @Test
    void testGetMatchById_Found() {
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match1));

        Optional<Match> found = matchService.getMatchById(1L);

        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getId());
        assertEquals(100L, found.get().getFootballDataMatchId());
        verify(matchRepository, times(1)).findById(1L);
    }

    @Test
    void testGetMatchById_NotFound() {
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Match> found = matchService.getMatchById(999L);

        assertFalse(found.isPresent());
        verify(matchRepository, times(1)).findById(999L);
    }

    @Test
    void testGetAllMatches() {
        when(matchRepository.findAll()).thenReturn(List.of(match1, match2, match3));

        List<Match> matches = matchService.getAllMatches();

        assertEquals(3, matches.size());
        assertEquals(match1, matches.get(0));
        assertEquals(match2, matches.get(1));
        assertEquals(match3, matches.get(2));
        verify(matchRepository, times(1)).findAll();
    }

    @Test
    void testGetAllMatches_Empty() {
        when(matchRepository.findAll()).thenReturn(List.of());

        List<Match> matches = matchService.getAllMatches();

        assertTrue(matches.isEmpty());
        verify(matchRepository, times(1)).findAll();
    }

    @Test
    void testGetMatchesByTeamId() {
        when(matchRepository.findByTeam1IdOrTeam2Id(1L, 1L))
                .thenReturn(List.of(match1, match2));

        List<Match> matches = matchService.getMatchesByTeamId(1L);

        assertEquals(2, matches.size());
        assertEquals(match1, matches.get(0));
        assertEquals(match2, matches.get(1));
        verify(matchRepository, times(1)).findByTeam1IdOrTeam2Id(1L, 1L);
    }

    @Test
    void testGetMatchesByTeamId_NoMatches() {
        when(matchRepository.findByTeam1IdOrTeam2Id(999L, 999L))
                .thenReturn(List.of());

        List<Match> matches = matchService.getMatchesByTeamId(999L);

        assertTrue(matches.isEmpty());
        verify(matchRepository, times(1)).findByTeam1IdOrTeam2Id(999L, 999L);
    }

    @Test
    void testUpdateMatch_Success() {
        Match updatedData = Match.builder()
                .footballDataMatchId(150L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(LocalDateTime.now().plusHours(1))
                .build();

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match1));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match arg = invocation.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        Match updated = matchService.updateMatch(1L, updatedData);

        assertNotNull(updated);
        assertEquals(1L, updated.getId());
        assertEquals(150L, updated.getFootballDataMatchId());
        assertEquals(10L, updated.getTeam1Id());
        assertEquals(20L, updated.getTeam2Id());
        verify(matchRepository, times(1)).findById(1L);
        verify(matchRepository, times(1)).save(any(Match.class));
    }

    @Test
    void testUpdateMatch_NotFound() {
        Match updatedData = Match.builder()
                .footballDataMatchId(150L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(LocalDateTime.now().plusHours(1))
                .build();

        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> matchService.updateMatch(999L, updatedData)
        );

        assertEquals("Match not found with id: 999", exception.getMessage());
        verify(matchRepository, times(1)).findById(999L);
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void testDeleteMatch_Success() {
        when(matchRepository.existsById(1L)).thenReturn(true);
        doNothing().when(matchRepository).deleteById(1L);

        assertDoesNotThrow(() -> matchService.deleteMatch(1L));

        verify(matchRepository, times(1)).existsById(1L);
        verify(matchRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteMatch_NotFound() {
        when(matchRepository.existsById(999L)).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> matchService.deleteMatch(999L)
        );

        assertEquals("Match not found with id: 999", exception.getMessage());
        verify(matchRepository, times(1)).existsById(999L);
        verify(matchRepository, never()).deleteById(anyLong());
    }

    @Test
    void testUpdateMatch_AllFields() {
        LocalDateTime newTime = LocalDateTime.now().plusDays(1);
        Match updatedData = Match.builder()
                .footballDataMatchId(200L)
                .team1Id(50L)
                .team2Id(60L)
                .matchTime(newTime)
                .build();

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match1));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Match updated = matchService.updateMatch(1L, updatedData);

        assertEquals(200L, updated.getFootballDataMatchId());
        assertEquals(50L, updated.getTeam1Id());
        assertEquals(60L, updated.getTeam2Id());
        assertEquals(newTime, updated.getMatchTime());
    }

    @Test
    void testCreateMultipleMatches() {
        when(matchRepository.save(match1)).thenReturn(match1);
        when(matchRepository.save(match2)).thenReturn(match2);
        when(matchRepository.save(match3)).thenReturn(match3);

        matchService.createMatch(match1);
        matchService.createMatch(match2);
        matchService.createMatch(match3);

        verify(matchRepository, times(3)).save(any(Match.class));
    }

    @Test
    void testGetMatchesByTeamId_PartialMatches() {
        when(matchRepository.findByTeam1IdOrTeam2Id(2L, 2L))
                .thenReturn(List.of(match1));

        List<Match> matches = matchService.getMatchesByTeamId(2L);

        assertEquals(1, matches.size());
        assertEquals(match1, matches.get(0));
    }
}
