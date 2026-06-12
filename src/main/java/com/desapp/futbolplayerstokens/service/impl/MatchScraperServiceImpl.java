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

    private final RestTemplate restTemplate;
    private final MatchService matchService;
    private final MatchRepository matchRepository;
    private final FootballDataProperties footballDataProperties;

    public MatchScraperServiceImpl(RestTemplate restTemplate, MatchService matchService, MatchRepository matchRepository, FootballDataProperties footballDataProperties) {
        this.restTemplate = restTemplate;
        this.matchService = matchService;
        this.matchRepository = matchRepository;
        this.footballDataProperties = footballDataProperties;
    }

    @Override
    public List<Match> scrapeMatchesOfToday() {
        // Limpiar tabla de matches antes de scrapear (excepto partidos en proceso, últimas 2h)
        LocalDateTime now = LocalDateTime.now();
        List<Match> existingMatches = matchRepository.findAll();
        List<Match> matchesToDelete = existingMatches.stream()
            .filter(m -> m.getMatchTime() == null || m.getMatchTime().isBefore(now.minusHours(2)))
            .toList();
        if (!matchesToDelete.isEmpty()) {
            matchRepository.deleteAll(matchesToDelete);
        }

        // Intenta obtener el token del .env, si no está disponible, usa System.getenv()
        String apiToken = getApiToken();

        if (apiToken == null || apiToken.isEmpty()) {
            throw new ConfigurationException("❌ FOOTBALL_DATA_API_TOKEN no configurado. Setea la variable de entorno FOOTBALL_DATA_API_TOKEN con tu token de football-data.org");
        }

        LocalDate today = LocalDate.now();
        String dateFrom = today.toString();
        String dateTo = today.plusDays(1).toString();

        logger.info("🔍 Scrapeando partidos para fecha: {} (Timezone: {})", dateFrom, ZoneId.systemDefault());

        List<Match> savedMatches = new ArrayList<>();

        for (String competitionId : footballDataProperties.getCompetitionIds()) {
            String url = String.format(
                "%s/competitions/%s/matches?dateFrom=%s&dateTo=%s",
                footballDataProperties.getBaseUrl(), competitionId, dateFrom, dateTo
            );

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Auth-Token", apiToken);

                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<MatchApiDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    MatchApiDTO.class
                );

                if (response.getBody() != null && response.getBody().getMatches() != null) {
                    for (MatchApiDTO.Match matchApi : response.getBody().getMatches()) {
                        Long team1Id = matchApi.getHomeTeam() != null ? matchApi.getHomeTeam().getId() : null;
                        Long team2Id = matchApi.getAwayTeam() != null ? matchApi.getAwayTeam().getId() : null;
                        LocalDateTime localMatchTime = matchApi.getMatchTime() != null
                            ? matchApi.getMatchTime().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                            : null;

                        if (localMatchTime != null && !localMatchTime.toLocalDate().equals(today)) {
                            logger.info("⏭️ Skipping match {} ({}): local date {} != today {}",
                                matchApi.getId(), localMatchTime, localMatchTime.toLocalDate(), today);
                            continue;
                        }

                        Match match = Match.builder()
                            .footballDataMatchId(matchApi.getId())
                            .status(matchApi.getStatus())
                            .matchTime(localMatchTime)
                            .team1Id(team1Id)
                            .team2Id(team2Id)
                            .build();

                        Match savedMatch = matchService.createMatch(match);
                        savedMatches.add(savedMatch);
                    }
                }

            } catch (Exception e) {
                logger.error("❌ Error al consultar la API para competencia {}: {}", competitionId, e.getMessage());
            }
        }

        return savedMatches;
    }

    /**
     * Obtiene el token de la API desde las variables de entorno (cargadas desde .env)
     */
    private String getApiToken() {
        // Intenta obtener desde System.getProperty() primero (cargado desde .env)
        String token = System.getProperty("FOOTBALL_DATA_API_TOKEN");
        if (token != null && !token.isEmpty()) {
            return token;
        }
        
        // Fallback a System.getenv() por si está seteado en el SO
        return System.getenv("FOOTBALL_DATA_API_TOKEN");
    }
}
