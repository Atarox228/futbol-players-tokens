package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Match;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MatchRepositoryTest {

    @Autowired
    private MatchRepository matchRepository;

    private Match match1;
    private Match match2;
    private Match match3;

    @BeforeEach
    void setUp() {
        // Limpiar la base de datos antes de cada test
        matchRepository.deleteAll();

        match1 = Match.builder()
                .footballDataMatchId(100L)
                .team1Id(1L)
                .team2Id(2L)
                .matchTime(LocalDateTime.now())
                .build();

        match2 = Match.builder()
                .footballDataMatchId(101L)
                .team1Id(1L)
                .team2Id(3L)
                .matchTime(LocalDateTime.now().plusHours(2))
                .build();

        match3 = Match.builder()
                .footballDataMatchId(102L)
                .team1Id(4L)
                .team2Id(5L)
                .matchTime(LocalDateTime.now().plusHours(4))
                .build();
    }

    @Test
    void testSaveMatch() {
        Match saved = matchRepository.save(match1);

        assertNotNull(saved.getId());
        assertEquals(100L, saved.getFootballDataMatchId());
        assertEquals(1L, saved.getTeam1Id());
        assertEquals(2L, saved.getTeam2Id());
    }

    @Test
    void testFindMatchById() {
        Match saved = matchRepository.save(match1);

        Optional<Match> found = matchRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(100L, found.get().getFootballDataMatchId());
    }

    @Test
    void testFindMatchByIdNotFound() {
        Optional<Match> found = matchRepository.findById(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void testFindAllMatches() {
        matchRepository.save(match1);
        matchRepository.save(match2);
        matchRepository.save(match3);

        List<Match> matches = matchRepository.findAll();

        assertEquals(3, matches.size());
    }

    @Test
    void testFindMatchesByTeam1Id() {
        matchRepository.save(match1);
        matchRepository.save(match2);
        matchRepository.save(match3);

        List<Match> matches = matchRepository.findByTeam1IdOrTeam2Id(1L, 1L);

        assertEquals(2, matches.size());
        assertTrue(matches.stream().anyMatch(m -> m.getFootballDataMatchId() == 100L));
        assertTrue(matches.stream().anyMatch(m -> m.getFootballDataMatchId() == 101L));
    }

    @Test
    void testFindMatchesByTeam2Id() {
        matchRepository.save(match1);
        matchRepository.save(match2);
        matchRepository.save(match3);

        List<Match> matches = matchRepository.findByTeam1IdOrTeam2Id(2L, 2L);

        assertEquals(1, matches.size());
        assertEquals(100L, matches.get(0).getFootballDataMatchId());
    }

    @Test
    void testFindMatchesByTeamIdMultipleResults() {
        matchRepository.save(match1);
        matchRepository.save(match2);
        matchRepository.save(match3);

        // Team 1 appears in match1 and match2
        List<Match> matches = matchRepository.findByTeam1IdOrTeam2Id(1L, 1L);

        assertEquals(2, matches.size());
    }

    @Test
    void testFindMatchesByTeamIdNoResults() {
        matchRepository.save(match1);
        matchRepository.save(match2);
        matchRepository.save(match3);

        List<Match> matches = matchRepository.findByTeam1IdOrTeam2Id(999L, 999L);

        assertTrue(matches.isEmpty());
    }

    @Test
    void testUpdateMatch() {
        Match saved = matchRepository.save(match1);

        saved.setTeam1Id(10L);
        saved.setTeam2Id(20L);
        Match updated = matchRepository.save(saved);

        assertEquals(10L, updated.getTeam1Id());
        assertEquals(20L, updated.getTeam2Id());
    }

    @Test
    void testDeleteMatch() {
        Match saved = matchRepository.save(match1);

        Long id = saved.getId();
        matchRepository.deleteById(id);

        Optional<Match> found = matchRepository.findById(id);
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsById() {
        Match saved = matchRepository.save(match1);

        assertTrue(matchRepository.existsById(saved.getId()));
        assertFalse(matchRepository.existsById(999L));
    }

    @Test
    void testCountMatches() {
        matchRepository.save(match1);
        matchRepository.save(match2);
        matchRepository.save(match3);

        long count = matchRepository.count();
        assertEquals(3, count);
    }

    @Test
    void testFindMatchesByTeamIdOrCondition() {
        // Team 1 in match1, Team 2 in match2, Team 4 in match3
        matchRepository.save(match1);  // team1=1, team2=2
        matchRepository.save(match2);  // team1=1, team2=3
        matchRepository.save(match3);  // team1=4, team2=5

        // Search for team 2: should find match1 (team2=2)
        List<Match> matches = matchRepository.findByTeam1IdOrTeam2Id(2L, 2L);

        assertEquals(1, matches.size());
        assertEquals(100L, matches.get(0).getFootballDataMatchId());
    }
}
