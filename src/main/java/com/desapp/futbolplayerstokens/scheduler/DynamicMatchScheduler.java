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
import java.time.Instant;
import java.time.Duration;
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
     * Para producción cambiar a: Duration.ofHours(2)
     */
    private void scheduleMatch(Match match) {
        long matchId = match.getId();
        Instant now = Instant.now();
        Instant executionInstant = now.plus(Duration.ofHours(2));
        LocalDateTime executionTime = LocalDateTime.ofInstant(executionInstant, ZoneId.systemDefault());

        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> {
                System.out.println("⏰ Ejecutando matcher task para partido: " + matchId + " a las " + LocalDateTime.now());
                sequentialExecutor.submit(() -> executeMatchTask(match));
            },
            executionInstant
        );

        scheduledMatches.put(matchId, future);
        scheduleInfo.put(matchId, new MatchScheduleInfo(matchId, match.getTeam1Id(), match.getTeam2Id(), executionTime));

        System.out.println("📅 Scheduler programado - Partido: " + matchId + " | Equipos: " + match.getTeam1Id() + " vs " + match.getTeam2Id() + " | Ejecución programada para: " + executionTime + " | Instant: " + executionInstant);
    }

    /**
     * Tarea que se ejecuta 2 horas después del partido
     */
    private void executeMatchTask(Match match) {
        try {
            System.out.println("🔍 Verificando estado del partido: " + match.getId());
            if (!isMatchFinished(match)) {
                System.out.println("⏸️ Partido " + match.getId() + " aún no terminado. Reprogramando para 10 minutos después...");
                rescheduleMatchIn10Minutes(match);
                return;
            }

            System.out.println("✅ Partido " + match.getId() + " está FINISHED");

            String team1Name = getTeamName(match.getTeam1Id());
            String team2Name = getTeamName(match.getTeam2Id());
            String league = getTeamLeague(match.getTeam1Id());

            System.out.println("📋 Team1: " + team1Name + " | Team2: " + team2Name + " | League: " + league);

            if (team1Name != null && team2Name != null && league != null) {
                System.out.println("🎯 Scrapeando Team1: " + team1Name);
                playerScraperService.scrapeTeamPlayersByName(team1Name, league);
                System.out.println("✅ Team1 scraped");

                System.out.println("🎯 Scrapeando Team2: " + team2Name);
                playerScraperService.scrapeTeamPlayersByName(team2Name, league);
                System.out.println("✅ Team2 scraped");
            } else {
                System.err.println("❌ No se pudieron obtener nombres/liga para el partido " + match.getId());
            }
        } catch (Exception e) {
            System.err.println("❌ Error scrapeando jugadores del partido: " + e.getMessage());
        } finally {
            scheduledMatches.remove(match.getId());
        }
    }

    /**
     * Reprograma el partido para 10 minutos después
     */
    private void rescheduleMatchIn10Minutes(Match match) {
        long matchId = match.getId();

        // Cancelar el scheduler anterior
        ScheduledFuture<?> oldFuture = scheduledMatches.get(matchId);
        if (oldFuture != null) {
            oldFuture.cancel(false);
        }

        Instant now = Instant.now();
        Instant executionInstant = now.plus(Duration.ofMinutes(10));
        LocalDateTime executionTime = LocalDateTime.ofInstant(executionInstant, ZoneId.systemDefault());

        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> {
                System.out.println("⏰ Reintentando match task para partido: " + matchId + " a las " + LocalDateTime.now());
                sequentialExecutor.submit(() -> executeMatchTask(match));
            },
            executionInstant
        );

        scheduledMatches.put(matchId, future);
        scheduleInfo.put(matchId, new MatchScheduleInfo(matchId, match.getTeam1Id(), match.getTeam2Id(), executionTime));

        System.out.println("🔄 Partido " + matchId + " reprogramado para: " + executionTime);
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
            System.out.println("🔗 Llamando a API: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<MatchApiDTO.Match> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                MatchApiDTO.Match.class
            );

            if (response.getBody() != null) {
                String status = response.getBody().getStatus();
                System.out.println("📊 Status de API para partido " + match.getId() + ": " + status);
                boolean isFinished = "FINISHED".equals(status);
                System.out.println("   ➜ ¿Terminado? " + isFinished);
                return isFinished;
            } else {
                System.err.println("❌ Response vacío de API");
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Error verificando estado del partido: " + e.getMessage());
            return false;
        }
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
