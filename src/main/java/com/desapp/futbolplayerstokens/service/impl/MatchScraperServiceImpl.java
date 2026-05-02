package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.config.FootballDataProperties;
import com.desapp.futbolplayerstokens.controller.dto.MatchApiDTO;
import com.desapp.futbolplayerstokens.modelo.Match;
import com.desapp.futbolplayerstokens.repository.MatchRepository;
import com.desapp.futbolplayerstokens.service.MatchScraperService;
import com.desapp.futbolplayerstokens.service.MatchService;
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
        // Limpiar tabla de matches antes de scrapear
        long count = matchRepository.count();
        if (count > 0) {
            System.out.println("\n🗑️ Limpiando tabla de matches... Eliminando " + count + " partidos");
            matchRepository.deleteAll();
            System.out.println("✓ Tabla limpiada\n");
        }

        String apiToken = footballDataProperties.getToken();
        if (apiToken == null || apiToken.isEmpty()) {
            throw new RuntimeException("❌ FOOTBALL_DATA_TOKEN no configurado. Setea la variable de entorno FOOTBALL_DATA_TOKEN con tu token de football-data.org");
        }

        LocalDate today = LocalDate.now();
        String dateFrom = today.toString();
        String dateTo = today.toString();

        List<Match> savedMatches = new ArrayList<>();

        for (String competitionId : footballDataProperties.getCompetitionIds()) {
            String url = String.format(
                "%s/competitions/%s/matches?dateFrom=%s&dateTo=%s",
                footballDataProperties.getBaseUrl(), competitionId, dateFrom, dateTo
            );

            System.out.println("📡 Consultando API para competencia " + competitionId + ": " + url);

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

                        Match match = Match.builder()
                            .footballDataMatchId(matchApi.getId())
                            .matchTime(localMatchTime)
                            .team1Id(team1Id)
                            .team2Id(team2Id)
                            .build();

                        Match savedMatch = matchService.createMatch(match);
                        savedMatches.add(savedMatch);
                        String team1Name = matchApi.getHomeTeam() != null ? matchApi.getHomeTeam().getName() : "Equipo desconocido";
                        String team2Name = matchApi.getAwayTeam() != null ? matchApi.getAwayTeam().getName() : "Equipo desconocido";
                        System.out.println("✓ Partido guardado: " + team1Name + " vs " + team2Name + " - " + localMatchTime + " (hora local)");
                    }
                } else {
                    System.out.println("⚠️ No hay partidos hoy en la competencia " + competitionId);
                }

            } catch (Exception e) {
                System.err.println("❌ Error al consultar la API para competencia " + competitionId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n✓ Se guardaron " + savedMatches.size() + " partidos de hoy");
        return savedMatches;
    }
}
