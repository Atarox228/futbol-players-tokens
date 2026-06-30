package com.desapp.futbolplayerstokens.scheduler;

import com.desapp.futbolplayerstokens.controller.dto.MatchApiDTO;
import com.desapp.futbolplayerstokens.modelo.Match;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.modelo.TeamEnum;
import com.desapp.futbolplayerstokens.repository.MatchRepository;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.config.FootballDataProperties;
import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import com.desapp.futbolplayerstokens.service.QuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ExecutorService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DynamicMatchScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DynamicMatchScheduler.class);
    private static final String STATUS_FINISHED = "FINISHED";

    private final TaskScheduler taskScheduler;
    private final MatchRepository matchRepository;
    private final RestTemplate restTemplate;
    private final PlayerScraperService playerScraperService;
    private final QuoteService quoteService;
    private final PlayerRepository playerRepository;
    private final FootballDataProperties footballDataProperties;
    private final ExecutorService sequentialExecutor;
    private final ExecutorService teamScraperExecutor;

    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledMatches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, MatchScheduleInfo> scheduleInfo = new ConcurrentHashMap<>();

    public DynamicMatchScheduler(TaskScheduler taskScheduler, MatchRepository matchRepository,
                                  RestTemplate restTemplate, PlayerScraperService playerScraperService,
                                  QuoteService quoteService, PlayerRepository playerRepository,
                                  FootballDataProperties footballDataProperties,
                                  ExecutorService teamScraperExecutor) {
        this.taskScheduler = taskScheduler;
        this.matchRepository = matchRepository;
        this.restTemplate = restTemplate;
        this.playerScraperService = playerScraperService;
        this.quoteService = quoteService;
        this.playerRepository = playerRepository;
        this.footballDataProperties = footballDataProperties;
        this.teamScraperExecutor = teamScraperExecutor;
        this.sequentialExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MatchTaskExecutor");
            t.setDaemon(false);
            return t;
        });
    }

    /**
     * Programa schedulers para cada partido del día
     */
    public void scheduleMatchesForToday() {
        cancelAllSchedules();

        var allMatches = matchRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Match match : allMatches) {
            if (match.getMatchTime() == null || STATUS_FINISHED.equals(match.getStatus())) continue;

            LocalDateTime checkTime = match.getMatchTime().plusHours(2);

            if (checkTime.isAfter(now)) {
                scheduleMatch(match);
            } else if (match.getMatchTime().isAfter(now.minusHours(4))) {
                rescheduleMatchIn10Minutes(match);
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
     * Programa un partido individual 2 horas después de su horario de comienzo
     */
    private void scheduleMatch(Match match) {
        long matchId = match.getId();
        
        // Obtener el horario del partido y agregar 2 horas
        LocalDateTime matchTime = match.getMatchTime();
        if (matchTime == null) {
            logger.warn("⚠️ Partido {} no tiene horario definido", matchId);
            return;
        }
        
        LocalDateTime executionTime = matchTime.plus(Duration.ofMinutes(130));
        Instant executionInstant = executionTime.atZone(ZoneId.systemDefault()).toInstant();

        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> {
                logger.info("⏰ Ejecutando matcher task para partido: {} a las {}", matchId, LocalDateTime.now());
                sequentialExecutor.submit(() -> executeMatchTask(match));
            },
            executionInstant
        );

        scheduledMatches.put(matchId, future);
        scheduleInfo.put(matchId, new MatchScheduleInfo(matchId, match.getTeam1Id(), match.getTeam2Id(), executionTime));

        logger.info("📅 Scheduler programado - Partido: {} | Equipos: {} vs {} | Horario partido: {} | Ejecución programada para: {}", matchId, formatTeam(match.getTeam1Id()), formatTeam(match.getTeam2Id()), matchTime, executionTime);
    }

    /**
     * Tarea que se ejecuta 2 horas después del partido
     */
    private void executeMatchTask(Match match) {
        try {
            logger.info("🔍 Verificando estado del partido: {}", match.getId());
            if (!isMatchFinished(match)) {
                logger.info("⏸️ Partido {} aún no terminado. Reprogramando para 10 minutos después...", match.getId());
                rescheduleMatchIn10Minutes(match);
                return;
            }

                logger.info("✅ Partido {} está {}", match.getId(), STATUS_FINISHED);

                match.setStatus(STATUS_FINISHED);
                matchRepository.save(match);

            String team1Name = getTeamName(match.getTeam1Id());
            String team2Name = getTeamName(match.getTeam2Id());
            String league = getTeamLeague(match.getTeam1Id());

            logger.info("📋 Team1: {} | Team2: {} | League: {}", team1Name, team2Name, league);

            if (team1Name != null && team2Name != null && league != null) {
                CompletableFuture<Void> team1Future = CompletableFuture.runAsync(() -> {
                    logger.info("🎯 Scrapeando Team1: {}", team1Name);
                    playerScraperService.scrapeTeamPlayersByName(team1Name, league);
                    logger.info("✅ Team1 scraped");
                }, teamScraperExecutor);

                CompletableFuture<Void> team2Future = CompletableFuture.runAsync(() -> {
                    logger.info("🎯 Scrapeando Team2: {}", team2Name);
                    playerScraperService.scrapeTeamPlayersByName(team2Name, league);
                    logger.info("✅ Team2 scraped");
                }, teamScraperExecutor);

                CompletableFuture.allOf(team1Future, team2Future).join();

                recalculateQuotesForTeam(team1Name);
                recalculateQuotesForTeam(team2Name);
            } else {
                logger.error("❌ No se pudieron obtener nombres/liga para el partido {}", match.getId());
            }
        } catch (Exception e) {
            logger.error("❌ Error scrapeando jugadores del partido: {}", e.getMessage());
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
                logger.info("⏰ Reintentando match task para partido: {} a las {}", matchId, LocalDateTime.now());
                sequentialExecutor.submit(() -> executeMatchTask(match));
            },
            executionInstant
        );

        scheduledMatches.put(matchId, future);
        scheduleInfo.put(matchId, new MatchScheduleInfo(matchId, match.getTeam1Id(), match.getTeam2Id(), executionTime));

        logger.info("🔄 Partido {} reprogramado para: {}", matchId, executionTime);
    }

    /**
     * Verifica si el partido está FINISHED consultando la API de football-data
     */
    private boolean isMatchFinished(Match match) {
        try {
            String apiToken = getApiToken();
            if (apiToken == null || apiToken.isEmpty()) {
                logger.error("❌ FOOTBALL_DATA_API_TOKEN no configurado");
                return false;
            }

            String url = String.format("https://api.football-data.org/v4/matches/%d", match.getFootballDataMatchId());
            logger.info("🔗 Llamando a API: {}", url);

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
                logger.info("📊 Status de API para partido {}: {}", match.getId(), status);
                boolean isFinished = STATUS_FINISHED.equals(status);
                logger.info("   ➜ ¿Terminado? {}", isFinished);
                return isFinished;
            } else {
                logger.error("❌ Response vacío de API");
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ Error verificando estado del partido: {}", e.getMessage());
            return false;
        }
    }

    private void recalculateQuotesForTeam(String teamName) {
        if (teamName == null) return;
        List<Player> players = playerRepository.findByTeamIgnoreCase(teamName);
        List<Long> playerIds = players.stream().map(Player::getId).toList();
        if (!playerIds.isEmpty()) {
            quoteService.recalculatePlayers(playerIds, QuoteTrigger.SCHEDULED);
            logger.info("📊 Cuotas recalculadas para {} jugadores de {}", playerIds.size(), teamName);
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

    private String formatTeam(Long teamId) {
        try {
            TeamEnum team = TeamEnum.fromId(teamId.intValue());
            return team.getName() + " (" + teamId + ")";
        } catch (Exception e) {
            return "Unknown (" + teamId + ")";
        }
    }

    /**
     * Cancela el scheduler de un partido específico y lo reprograma para ejecutarse en 5 segundos.
     * Útil para forzar el scrapeo de un partido que ya se jugó pero cuyo scheduler no se ejecutó.
     */
    public void rescheduleMatchImmediately(Long matchId) {
        ScheduledFuture<?> existing = scheduledMatches.get(matchId);
        if (existing != null) {
            existing.cancel(false);
            logger.info("❌ Scheduler existente cancelado para partido {}", matchId);
        }

        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado: " + matchId));

        Instant executionInstant = Instant.now().plus(Duration.ofSeconds(5));
        LocalDateTime executionTime = LocalDateTime.ofInstant(executionInstant, ZoneId.systemDefault());

        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> {
                logger.info("⚡ Ejecutando match task forzada para partido: {} a las {}", matchId, LocalDateTime.now());
                sequentialExecutor.submit(() -> executeMatchTask(match));
            },
            executionInstant
        );

        scheduledMatches.put(matchId, future);
        scheduleInfo.put(matchId, new MatchScheduleInfo(matchId, match.getTeam1Id(), match.getTeam2Id(), executionTime));

        logger.info("⚡ Partido {} reprogramado forzadamente para ejecutarse en 5 segundos ({})", matchId, executionTime);
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

    /**
     * Obtiene el token de la API desde las variables de entorno (cargadas desde .env)
     */
    private String getApiToken() {
        String token = System.getenv("FOOTBALL_DATA_API_TOKEN");
        if (token != null && !token.isBlank()) {
            return token;
        }

        token = System.getProperty("FOOTBALL_DATA_API_TOKEN");
        if (token != null && !token.isBlank()) {
            return token;
        }

        token = footballDataProperties.getToken();
        if (token != null && !token.isBlank()) {
            return token;
        }

        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().trim().equalsIgnoreCase("FOOTBALL_DATA_API_TOKEN")) {
                String val = entry.getValue();
                if (val != null && !val.isBlank()) {
                    return val;
                }
            }
        }

        return null;
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
