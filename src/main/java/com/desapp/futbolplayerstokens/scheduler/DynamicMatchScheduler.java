package com.desapp.futbolplayerstokens.scheduler;

import com.desapp.futbolplayerstokens.controller.dto.MatchApiDTO;
import com.desapp.futbolplayerstokens.modelo.Match;
import com.desapp.futbolplayerstokens.modelo.TeamEnum;
import com.desapp.futbolplayerstokens.repository.MatchRepository;
import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ExecutorService;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

@Service
public class DynamicMatchScheduler {

    private final TaskScheduler taskScheduler;
    private final MatchRepository matchRepository;
    private final RestTemplate restTemplate;
    private final PlayerScraperService playerScraperService;
    private final ExecutorService sequentialExecutor;

    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledMatches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, MatchScheduleInfo> scheduleInfo = new ConcurrentHashMap<>();

    public DynamicMatchScheduler(TaskScheduler taskScheduler, MatchRepository matchRepository,
                                  RestTemplate restTemplate, PlayerScraperService playerScraperService) {
        this.taskScheduler = taskScheduler;
        this.matchRepository = matchRepository;
        this.restTemplate = restTemplate;
        this.playerScraperService = playerScraperService;
        this.sequentialExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MatchTaskExecutor");
            t.setDaemon(false);
            return t;
        });
    }

    /**
     * Programa schedulers para cada partido del día (solo futuros)
     */
    public void scheduleMatchesForToday() {
        cancelAllSchedules();

        var allMatches = matchRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Match match : allMatches) {
            if (match.getMatchTime() != null && match.getMatchTime().isAfter(now)) {
                scheduleMatch(match);
            }
        }
    }

    /**
     * Programa TODOS los partidos (incluyendo los pasados) - SOLO PARA TESTING
     */
    public void scheduleAllMatchesForTesting() {
        cancelAllSchedules();

        var allMatches = matchRepository.findAll();

        for (Match match : allMatches) {
            if (match.getMatchTime() != null) {
                scheduleMatch(match);
            }
        }
    }

    /**
     * Programa un partido individual 1 minuto después de agregarse (testing)
     * Para producción cambiar a: match.getMatchTime().plusHours(2)
     */
    private void scheduleMatch(Match match) {
        long matchId = match.getId();
        LocalDateTime executionTime = LocalDateTime.now().plusHours(2);

        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> sequentialExecutor.submit(() -> executeMatchTask(match)),
            executionTime.atZone(ZoneId.systemDefault()).toInstant()
        );

        scheduledMatches.put(matchId, future);
        scheduleInfo.put(matchId, new MatchScheduleInfo(matchId, match.getTeam1Id(), match.getTeam2Id(), executionTime));

        System.out.println("📅 Scheduler programado - Partido: " + matchId + " | Equipos: " + match.getTeam1Id() + " vs " + match.getTeam2Id() + " | Ejecución: " + executionTime);
    }

    /**
     * Tarea que se ejecuta 2 horas después del partido
     */
    private void executeMatchTask(Match match) {
        try {
            if (!isMatchFinished(match)) {
                return;
            }

            String team1Name = getTeamName(match.getTeam1Id());
            String team2Name = getTeamName(match.getTeam2Id());
            String league = getTeamLeague(match.getTeam1Id());

            if (team1Name != null && team2Name != null && league != null) {
                playerScraperService.scrapeTeamPlayersByName(team1Name, league);
                playerScraperService.scrapeTeamPlayersByName(team2Name, league);
            }
        } catch (Exception e) {
            System.err.println("❌ Error scrapeando jugadores del partido: " + e.getMessage());
        } finally {
            scheduledMatches.remove(match.getId());
        }
    }

    /**
     * Verifica si el partido está FINISHED consultando la API de football-data
     */
    private boolean isMatchFinished(Match match) {
        try {
            String apiToken = System.getenv("FOOTBALL_DATA_API_TOKEN");
            if (apiToken == null || apiToken.isEmpty()) {
                System.err.println("❌ FOOTBALL_DATA_API_TOKEN no configurado");
                return false;
            }

            String url = String.format("https://api.football-data.org/v4/matches/%d", match.getFootballDataMatchId());

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<MatchApiDTO.Match> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                MatchApiDTO.Match.class
            );

            if (response.getBody() != null && response.getBody().getStatus() != null) {
                return "FINISHED".equals(response.getBody().getStatus());
            }
        } catch (Exception e) {
            System.err.println("❌ Error verificando estado del partido: " + e.getMessage());
        }
        return false;
    }

    private String getTeamName(Long teamId) {
        try {
            return TeamEnum.fromId(teamId.intValue()).getName();
        } catch (Exception e) {
            return null;
        }
    }

    private String getTeamLeague(Long teamId) {
        try {
            return TeamEnum.fromId(teamId.intValue()).getLeague();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Cancela todos los schedulers activos
     */
    public void cancelAllSchedules() {
        for (ScheduledFuture<?> future : scheduledMatches.values()) {
            future.cancel(false);
        }
        scheduledMatches.clear();
        scheduleInfo.clear();
    }

    /**
     * Obtiene cuántos partidos están programados
     */
    public int getScheduledMatchCount() {
        return scheduledMatches.size();
    }

    /**
     * Obtiene información de todos los schedulers programados
     */
    public List<MatchScheduleInfo> getAllScheduledMatches() {
        return new ArrayList<>(scheduleInfo.values());
    }

    public static class MatchScheduleInfo {
        public Long matchId;
        public Long team1Id;
        public Long team2Id;
        public LocalDateTime executionTime;
        public String status;

        public MatchScheduleInfo(Long matchId, Long team1Id, Long team2Id, LocalDateTime executionTime) {
            this.matchId = matchId;
            this.team1Id = team1Id;
            this.team2Id = team2Id;
            this.executionTime = executionTime;
            this.status = "SCHEDULED";
        }

        @Override
        public String toString() {
            return "MatchScheduleInfo{" +
                    "matchId=" + matchId +
                    ", team1Id=" + team1Id +
                    ", team2Id=" + team2Id +
                    ", executionTime=" + executionTime +
                    ", status='" + status + '\'' +
                    '}';
        }
    }
}
