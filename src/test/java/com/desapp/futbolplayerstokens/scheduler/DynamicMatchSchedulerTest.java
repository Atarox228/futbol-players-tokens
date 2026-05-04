package com.desapp.futbolplayerstokens.scheduler;

import com.desapp.futbolplayerstokens.modelo.Match;
import com.desapp.futbolplayerstokens.repository.MatchRepository;
import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Tests del DynamicMatchScheduler")
class DynamicMatchSchedulerTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PlayerScraperService playerScraperService;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private DynamicMatchScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduler = new DynamicMatchScheduler(taskScheduler, matchRepository, restTemplate, playerScraperService);
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenReturn((ScheduledFuture) scheduledFuture);
    }

    @Test
    @DisplayName("Debe programar solo partidos con horario futuro")
    void testScheduleMatchesForTodayWithFutureMatches() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusHours(3);
        LocalDateTime past = now.minusHours(1);

        Match futureMatch = Match.builder()
            .id(1L)
            .footballDataMatchId(1L)
            .team1Id(81L)
            .team2Id(86L)
            .matchTime(future)
            .build();

        Match pastMatch = Match.builder()
            .id(2L)
            .footballDataMatchId(2L)
            .team1Id(78L)
            .team2Id(90L)
            .matchTime(past)
            .build();

        when(matchRepository.findAll()).thenReturn(Arrays.asList(futureMatch, pastMatch));
        scheduler.scheduleMatchesForToday();

        assertEquals(1, scheduler.getScheduledMatchCount());
    }

    @Test
    @DisplayName("Debe programar todos los partidos en modo testing")
    void testScheduleAllMatchesForTesting() {
        LocalDateTime now = LocalDateTime.now();

        Match match1 = Match.builder()
            .id(1L)
            .footballDataMatchId(1L)
            .team1Id(81L)
            .team2Id(86L)
            .matchTime(now.minusHours(2))
            .build();

        Match match2 = Match.builder()
            .id(2L)
            .footballDataMatchId(2L)
            .team1Id(78L)
            .team2Id(90L)
            .matchTime(now.minusHours(1))
            .build();

        when(matchRepository.findAll()).thenReturn(Arrays.asList(match1, match2));
        scheduler.scheduleAllMatchesForTesting();

        assertEquals(2, scheduler.getScheduledMatchCount());
    }

    @Test
    @DisplayName("Debe cancelar todos los schedulers")
    void testCancelAllSchedules() {
        LocalDateTime future = LocalDateTime.now().plusHours(3);

        Match match = Match.builder()
            .id(1L)
            .footballDataMatchId(1L)
            .team1Id(81L)
            .team2Id(86L)
            .matchTime(future)
            .build();

        when(matchRepository.findAll()).thenReturn(Arrays.asList(match));
        scheduler.scheduleMatchesForToday();
        assertEquals(1, scheduler.getScheduledMatchCount());

        scheduler.cancelAllSchedules();
        assertEquals(0, scheduler.getScheduledMatchCount());
    }

    @Test
    @DisplayName("Debe guardar información de los schedulers programados")
    void testScheduleInfoStorage() {
        LocalDateTime future = LocalDateTime.now().plusHours(3);

        Match match = Match.builder()
            .id(1L)
            .footballDataMatchId(1L)
            .team1Id(81L)
            .team2Id(86L)
            .matchTime(future)
            .build();

        when(matchRepository.findAll()).thenReturn(Arrays.asList(match));
        scheduler.scheduleMatchesForToday();

        List<DynamicMatchScheduler.MatchScheduleInfo> allMatches = scheduler.getAllScheduledMatches();
        assertEquals(1, allMatches.size());
        assertEquals(1L, allMatches.get(0).matchId);
        assertEquals(81L, allMatches.get(0).team1Id);
        assertEquals(86L, allMatches.get(0).team2Id);
        assertEquals("SCHEDULED", allMatches.get(0).status);
    }

    @Test
    @DisplayName("No debe programar partidos sin horario")
    void testIgnoreMatchesWithoutTime() {
        Match matchWithoutTime = Match.builder()
            .id(1L)
            .footballDataMatchId(1L)
            .team1Id(81L)
            .team2Id(86L)
            .matchTime(null)
            .build();

        when(matchRepository.findAll()).thenReturn(Arrays.asList(matchWithoutTime));
        scheduler.scheduleMatchesForToday();

        assertEquals(0, scheduler.getScheduledMatchCount());
    }

    @Test
    @DisplayName("Debe limpiar información al cancelar schedulers")
    void testCancelClearsScheduleInfo() {
        LocalDateTime future = LocalDateTime.now().plusHours(3);

        Match match = Match.builder()
            .id(1L)
            .footballDataMatchId(1L)
            .team1Id(81L)
            .team2Id(86L)
            .matchTime(future)
            .build();

        when(matchRepository.findAll()).thenReturn(Arrays.asList(match));
        scheduler.scheduleMatchesForToday();
        assertEquals(1, scheduler.getAllScheduledMatches().size());

        scheduler.cancelAllSchedules();
        assertEquals(0, scheduler.getAllScheduledMatches().size());
    }
}
