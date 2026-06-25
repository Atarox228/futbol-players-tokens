package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.config.FootballDataProperties;
import com.desapp.futbolplayerstokens.controller.dto.MatchApiDTO;
import com.desapp.futbolplayerstokens.exception.ConfigurationException;
import com.desapp.futbolplayerstokens.modelo.Match;
import com.desapp.futbolplayerstokens.repository.MatchRepository;
import com.desapp.futbolplayerstokens.service.MatchScraperService;
import com.desapp.futbolplayerstokens.service.MatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchScraperServiceImpl implements MatchScraperService {

    private static final Logger logger = LoggerFactory.getLogger(MatchScraperServiceImpl.class);
    private static final String STATUS_FINISHED = "FINISHED";

    private final RestTemplate restTemplate;
    private final MatchService matchService;
    private final MatchRepository matchRepository;
    private final FootballDataProperties footballDataProperties;
    private final Environment environment;

    public MatchScraperServiceImpl(RestTemplate restTemplate, MatchService matchService, MatchRepository matchRepository, FootballDataProperties footballDataProperties, Environment environment) {
        this.restTemplate = restTemplate;
        this.matchService = matchService;
        this.matchRepository = matchRepository;
        this.footballDataProperties = footballDataProperties;
        this.environment = environment;
    }

    @Override
    public List<Match> scrapeMatchesOfToday() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);
        cleanOldMatches(now);

        String apiToken = getApiToken();
        if (apiToken == null || apiToken.isEmpty()) {
            throw new ConfigurationException("❌ FOOTBALL_DATA_API_TOKEN no configurado. Setea la variable de entorno FOOTBALL_DATA_API_TOKEN con tu token de football-data.org");
        }

        LocalDate today = LocalDate.now(zone);
        logger.info("🔍 Scrapeando partidos para fecha: {} (Timezone: {})", today, zone);

        List<Match> savedMatches = new ArrayList<>();

        for (String competitionId : footballDataProperties.getCompetitionIds()) {
            fetchAndSaveMatches(competitionId, today, apiToken, savedMatches);
        }

        return savedMatches;
    }

    private void cleanOldMatches(LocalDateTime now) {
        List<Match> existingMatches = matchRepository.findAll();
        List<Match> matchesToDelete = existingMatches.stream()
            .filter(m -> m.getMatchTime() == null || (m.getMatchTime().isBefore(now.minusHours(2)) && STATUS_FINISHED.equals(m.getStatus())))
            .toList();
        if (!matchesToDelete.isEmpty()) {
            matchRepository.deleteAll(matchesToDelete);
        }
    }

    private void fetchAndSaveMatches(String competitionId, LocalDate today, String apiToken, List<Match> savedMatches) {
        String url = String.format(
            "%s/competitions/%s/matches?dateFrom=%s&dateTo=%s",
            footballDataProperties.getBaseUrl(), competitionId, today, today.plusDays(1)
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<MatchApiDTO> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, MatchApiDTO.class
            );

            if (response.getBody() == null || response.getBody().getMatches() == null) return;

            for (MatchApiDTO.Match matchApi : response.getBody().getMatches()) {
                processMatchApi(matchApi, today, savedMatches);
            }

        } catch (Exception e) {
            logger.error("❌ Error al consultar la API para competencia {}: {}", competitionId, e.getMessage());
        }
    }

    private void processMatchApi(MatchApiDTO.Match matchApi, LocalDate today, List<Match> savedMatches) {
        Long team1Id = matchApi.getHomeTeam() != null ? matchApi.getHomeTeam().getId() : null;
        Long team2Id = matchApi.getAwayTeam() != null ? matchApi.getAwayTeam().getId() : null;
        LocalDateTime localMatchTime = matchApi.getMatchTime() != null
            ? matchApi.getMatchTime().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
            : null;

        if (localMatchTime != null && !localMatchTime.toLocalDate().equals(today)) {
            logger.info("⏭️ Skipping match {} ({}): local date {} != today {}",
                matchApi.getId(), localMatchTime, localMatchTime.toLocalDate(), today);
            return;
        }

        Match match = Match.builder()
            .footballDataMatchId(matchApi.getId())
            .status(matchApi.getStatus())
            .matchTime(localMatchTime)
            .team1Id(team1Id)
            .team2Id(team2Id)
            .build();

        savedMatches.add(matchService.createMatch(match));
    }

    /**
     * Obtiene el token de la API desde las variables de entorno (cargadas desde .env)
     */
    private String getApiToken() {
        String envVal = System.getenv("FOOTBALL_DATA_API_TOKEN");
        logger.warn("DEBUG getApiToken: System.getenv(FOOTBALL_DATA_API_TOKEN)='{}'", envVal);

        if (envVal != null && !envVal.isBlank()) {
            logger.info("Using FOOTBALL_DATA_API_TOKEN from System.getenv");
            return envVal;
        }

        String propVal = environment.getProperty("FOOTBALL_DATA_API_TOKEN");
        if (propVal != null && !propVal.isBlank()) {
            logger.info("Using FOOTBALL_DATA_API_TOKEN from Spring Environment");
            return propVal;
        }

        String cfgVal = footballDataProperties.getToken();
        if (cfgVal != null && !cfgVal.isBlank()) {
            logger.info("Using FOOTBALL_DATA_API_TOKEN from FootballDataProperties");
            return cfgVal;
        }

        String sysVal = System.getProperty("FOOTBALL_DATA_API_TOKEN");
        if (sysVal != null && !sysVal.isBlank()) {
            logger.info("Using FOOTBALL_DATA_API_TOKEN from System.getProperty");
            return sysVal;
        }

        return null;
    }
}
