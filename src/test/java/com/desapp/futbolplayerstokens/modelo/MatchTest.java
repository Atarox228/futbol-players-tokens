package com.desapp.futbolplayerstokens.modelo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MatchTest {

    private Match match;

    @BeforeEach
    void setUp() {
        match = new Match();
    }

    @Test
    void testMatchDefaultConstructor() {
        assertNotNull(match);
        assertNull(match.getId());
        assertNull(match.getFootballDataMatchId());
        assertNull(match.getTeam1Id());
        assertNull(match.getTeam2Id());
        assertNull(match.getMatchTime());
    }

    @Test
    void testMatchAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Match match = new Match(1L, 100L, 10L, 20L, now, "TIMED");

        assertEquals(1L, match.getId());
        assertEquals(100L, match.getFootballDataMatchId());
        assertEquals(10L, match.getTeam1Id());
        assertEquals(20L, match.getTeam2Id());
        assertEquals(now, match.getMatchTime());
    }

    @Test
    void testMatchBuilder() {
        LocalDateTime now = LocalDateTime.now();
        Match built = Match.builder()
                .id(1L)
                .footballDataMatchId(100L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(now)
                .build();

        assertEquals(1L, built.getId());
        assertEquals(100L, built.getFootballDataMatchId());
        assertEquals(10L, built.getTeam1Id());
        assertEquals(20L, built.getTeam2Id());
        assertEquals(now, built.getMatchTime());
    }

    @Test
    void testMatchSetters() {
        LocalDateTime now = LocalDateTime.now();

        match.setId(1L);
        match.setFootballDataMatchId(100L);
        match.setTeam1Id(10L);
        match.setTeam2Id(20L);
        match.setMatchTime(now);

        assertEquals(1L, match.getId());
        assertEquals(100L, match.getFootballDataMatchId());
        assertEquals(10L, match.getTeam1Id());
        assertEquals(20L, match.getTeam2Id());
        assertEquals(now, match.getMatchTime());
    }

    @Test
    void testMatchGetters() {
        LocalDateTime now = LocalDateTime.now();
        match.setId(5L);
        match.setFootballDataMatchId(200L);
        match.setTeam1Id(15L);
        match.setTeam2Id(25L);
        match.setMatchTime(now);

        assertEquals(5L, match.getId());
        assertEquals(200L, match.getFootballDataMatchId());
        assertEquals(15L, match.getTeam1Id());
        assertEquals(25L, match.getTeam2Id());
        assertEquals(now, match.getMatchTime());
    }

    @Test
    void testMatchEquality() {
        LocalDateTime now = LocalDateTime.now();
        Match match1 = Match.builder()
                .id(1L)
                .footballDataMatchId(100L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(now)
                .build();

        Match match2 = Match.builder()
                .id(1L)
                .footballDataMatchId(100L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(now)
                .build();

        assertEquals(match1, match2);
    }

    @Test
    void testMatchInequality() {
        LocalDateTime now = LocalDateTime.now();
        Match match1 = Match.builder()
                .id(1L)
                .footballDataMatchId(100L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(now)
                .build();

        Match match2 = Match.builder()
                .id(2L)
                .footballDataMatchId(100L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(now)
                .build();

        assertNotEquals(match1, match2);
    }

    @Test
    void testMatchToString() {
        LocalDateTime now = LocalDateTime.now();
        Match match = Match.builder()
                .id(1L)
                .footballDataMatchId(100L)
                .team1Id(10L)
                .team2Id(20L)
                .matchTime(now)
                .build();

        String toString = match.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Match"));
    }
}
