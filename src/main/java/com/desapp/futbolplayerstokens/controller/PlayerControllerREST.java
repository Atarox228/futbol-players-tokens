package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;
import com.desapp.futbolplayerstokens.exception.ValidationException;
import com.desapp.futbolplayerstokens.modelo.LeagueConstant;
import com.desapp.futbolplayerstokens.modelo.TeamEnum;
import com.desapp.futbolplayerstokens.controller.dto.PlayerDetailDTO;
import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.controller.dto.PlayerRankingDTO;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.service.PlayerService;
import com.desapp.futbolplayerstokens.service.QuoteService;
import com.desapp.futbolplayerstokens.service.RankingService;

import jakarta.annotation.security.PermitAll;

import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/players")
@Tag(name = "Players", description = "Endpoints para gestión de jugadores de fútbol")
public class PlayerControllerREST {

    private final PlayerService playerService;
    private final PlayerScraperService scraperService;
    private final QuoteService quoteService;
    private final RankingService rankingService;
    private final PlayerRepository playerRepository;

    public PlayerControllerREST(PlayerService playerService,
                                PlayerScraperService scraperService,
                                QuoteService quoteService,
                                RankingService rankingService,
                                PlayerRepository playerRepository) {
        this.playerService = playerService;
        this.scraperService = scraperService;
        this.quoteService = quoteService;
        this.rankingService = rankingService;
        this.playerRepository = playerRepository;
    }

    @GetMapping("/hello")
    @Operation(summary = "Health check", description = "Endpoint simple para verificar que el servicio está activo")
    public String hello() {
        return "Hello World";
    }

    @GetMapping
    @Operation(summary = "Obtener jugadores", description = "Retorna una lista de jugadores con filtros opcionales por liga, equipo y posición")
    public ResponseEntity<Page<PlayerDTO>> getPlayers(
            @Parameter(description = "Liga de los jugadores (ej: LALIGA)")
            @RequestParam(required = false) String league,
            @Parameter(description = "Equipo de los jugadores")
            @RequestParam(required = false) String team,
            @Parameter(description = "Posición del jugador en el campo")
            @RequestParam(required = false) String position,
            @Parameter(description = "Parámetros de paginación")
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        try {
            Page<PlayerDTO> players = playerService.getPlayersWithFilters(league, team, position, pageable);
            return ResponseEntity.ok(players);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/ranking")
    @Operation(summary = "Obtener ranking de jugadores", description = "Retorna un ranking paginado de jugadores ordenados por puntuación")
    public ResponseEntity<List<PlayerRankingDTO>> getRanking(
            @Parameter(description = "Número de página (comienza en 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de registros por página")
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<PlayerRankingDTO> ranking = rankingService.getRanking(page, size);
            return ResponseEntity.ok(ranking);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "Obtener jugador por ID", description = "Retorna la información detallada de un jugador específico")
    public ResponseEntity<PlayerDetailDTO> getPlayer(
            @Parameter(description = "ID del jugador")
            @PathVariable Long id) {
        try {
            PlayerDetailDTO player = playerService.getPlayerById(id);
            return ResponseEntity.ok(player);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id:[0-9]+}/quotes")
    @Operation(summary = "Obtener cotizaciones de un jugador", description = "Retorna el historial de cotizaciones de un jugador específico")
    public ResponseEntity<List<QuoteDTO>> getQuotes(
            @Parameter(description = "ID del jugador")
            @PathVariable Long id) {
        try {
            List<QuoteDTO> quotes = quoteService.getQuotesByPlayerId(id);
            return ResponseEntity.ok(quotes);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @PostMapping("/scrape")
    @PermitAll
    @Operation(summary = "Raspar y actualizar jugadores", description = "Extrae datos de jugadores de fuentes externas y los almacena en la base de datos")
    public ResponseEntity<String> scrapeAndSavePlayers() {
        try {
            long startTime = System.currentTimeMillis();

            // Delegate orchestration to the service which will clear DB and run the rich team-based scraper
            scraperService.scrapeAllPlayersForce();

            quoteService.recalculateAll(QuoteTrigger.MANUAL);

            long duration = System.currentTimeMillis() - startTime;
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;

            String message = String.format(
                "✓ Scraping forzado completado correctamente%n⏱️ Tiempo total: %d min %d seg",
                minutes,
                seconds
            );

            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Error: " + e.getMessage());
        }
    }

    @PostMapping("/scrape/new-only")
    @PermitAll
    public ResponseEntity<String> scrapeAndSaveNewPlayersOnly() {
        try {
            long startTime = System.currentTimeMillis();

            // Definir las ligas a scrapear - Map de nombre de liga + URL
            Map<String, String> ligas = new LinkedHashMap<>();
            ligas.put(LeagueConstant.LALIGA, "https://es.whoscored.com/regions/206/tournaments/4/seasons/10803/stages/24622/playerstatistics/espa%C3%B1a-laliga-2025-2026");
            ligas.put(LeagueConstant.PREMIER_LEAGUE, "https://es.whoscored.com/regions/252/tournaments/2/seasons/10743/stages/24533/playerstatistics/inglaterra-premier-league-2025-2026");
            ligas.put(LeagueConstant.BUNDESLIGA, "https://es.whoscored.com/regions/81/tournaments/3/seasons/10720/stages/24478/playerstatistics/alemania-bundesliga-2025-2026");
            ligas.put(LeagueConstant.SERIE_A, "https://es.whoscored.com/regions/108/tournaments/5/seasons/10732/stages/24500/playerstatistics/italia-serie-a-2025-2026");
            ligas.put(LeagueConstant.LIGUE_1, "https://es.whoscored.com/regions/74/tournaments/22/seasons/10792/stages/24609/playerstatistics/francia-ligue-1-2025-2026");

            int totalJugadoresNuevos = 0;
            int[] totalGuardados = {0};

            for (Map.Entry<String, String> liga : ligas.entrySet()) {
                // Callback que guarda solo los jugadores nuevos de cada página
                var jugadoresNuevos = scraperService.scrapeNewPlayersOnly(
                    liga.getValue(),
                    liga.getKey(),
                    playersPage -> {
                        playerService.saveAllPlayers(playersPage);
                        totalGuardados[0] += playersPage.size();
                    }
                );

                totalJugadoresNuevos += jugadoresNuevos.size();
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;

            String message = String.format(
                "✓ Se encontraron y guardaron %d jugadores NUEVOS de todas las ligas%n⏱️ Tiempo total: %d min %d seg",
                totalJugadoresNuevos,
                minutes,
                seconds
            );

            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Error: " + e.getMessage());
        }
    }


    @PostMapping("/scrape/team/{id}")
    @PermitAll
    public ResponseEntity<String> scrapeAndOverwriteTeamPlayers(@PathVariable Integer id) {
        try {
            TeamEnum teamEnum = TeamEnum.fromId(id);
            String teamName = teamEnum.getName();
            String league = teamEnum.getLeague();

            long startTime = System.currentTimeMillis();

            scraperService.scrapeTeamPlayersByName(teamName, league);

            List<Player> teamPlayers = playerRepository.findByTeamIgnoreCase(teamName);
            List<Long> playerIds = teamPlayers.stream().map(Player::getId).toList();
            if (!playerIds.isEmpty()) {
                quoteService.recalculatePlayers(playerIds, QuoteTrigger.MANUAL);
            }

            long duration = System.currentTimeMillis() - startTime;
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;

            String message = String.format(
                "✓ Scraping completado para %s. Jugadores procesados. Tiempo: %d min %d seg",
                teamName,
                minutes,
                seconds
            );

            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("❌ ID de equipo inválida: " + id);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Error al scrapear equipo: " + e.getMessage());
        }
    }

    @PostMapping("/scrape/league/{league}")
    @PermitAll
    public ResponseEntity<String> scrapeAllTeamsByLeague(@PathVariable String league) {
        try {
            String normalizedLeague = league == null ? "" : league.trim();
            String starterTeam = starterTeamByLeague(normalizedLeague);

            long startTime = System.currentTimeMillis();
            scraperService.scrapeLeaguePlayersByStarterTeam(starterTeam, normalizedLeague);

            List<Player> leaguePlayers = playerRepository.findByFilters(normalizedLeague, null, null, Pageable.unpaged()).getContent();
            List<Long> playerIds = leaguePlayers.stream().map(Player::getId).toList();
            if (!playerIds.isEmpty()) {
                quoteService.recalculatePlayers(playerIds, QuoteTrigger.MANUAL);
            }

            long duration = System.currentTimeMillis() - startTime;
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;

            String message = String.format(
                "✓ Scraping completo de %s finalizado (todos los equipos). Tiempo: %d min %d seg",
                normalizedLeague,
                minutes,
                seconds
            );

            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("❌ Liga inválida. Usá: LaLiga, Premier League, Bundesliga, Serie A, Ligue 1, World Cup");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Error al scrapear liga: " + e.getMessage());
        }
    }

    private String starterTeamByLeague(String league) {
        String normalized = league.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "laliga" -> "Athletic Club";
            case "premier league" -> "Manchester City";
            case "bundesliga" -> "Union Berlin";
            case "serie a" -> "Inter";
            case "ligue 1" -> "Paris Saint-Germain";
            case "world cup" -> "Mexico";
            default -> throw new ValidationException("Liga no soportada: " + league);
        };
    }


}
