package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;
import com.desapp.futbolplayerstokens.modelo.TeamEnum;
import com.desapp.futbolplayerstokens.controller.dto.PlayerDetailDTO;
import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.controller.dto.PlayerRankingDTO;
import com.desapp.futbolplayerstokens.service.PlayerService;
import com.desapp.futbolplayerstokens.service.QuoteService;
import com.desapp.futbolplayerstokens.service.RankingService;

import jakarta.annotation.security.PermitAll;

import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/players")
public class PlayerControllerREST {

    private final PlayerService playerService;
    private final PlayerScraperService scraperService;
    private final QuoteService quoteService;
    private final RankingService rankingService;

    public PlayerControllerREST(PlayerService playerService,
                                PlayerScraperService scraperService,
                                QuoteService quoteService,
                                RankingService rankingService) {
        this.playerService = playerService;
        this.scraperService = scraperService;
        this.quoteService = quoteService;
        this.rankingService = rankingService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello World";
    }

    @GetMapping
    public ResponseEntity<List<PlayerDTO>> getPlayers(
            @RequestParam(required = false) String league,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String position) {
        try {
            List<PlayerDTO> players = playerService.getPlayersWithFilters(league, team, position);
            return ResponseEntity.ok(players);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<PlayerRankingDTO>> getRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<PlayerRankingDTO> ranking = rankingService.getRanking(page, size);
            return ResponseEntity.ok(ranking);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerDetailDTO> getPlayer(@PathVariable Long id) {
        try {
            PlayerDetailDTO player = playerService.getPlayerById(id);
            return ResponseEntity.ok(player);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/quotes")
    public ResponseEntity<List<QuoteDTO>> getQuotes(@PathVariable Long id) {
        try {
            List<QuoteDTO> quotes = quoteService.getQuotesByPlayerId(id);
            return ResponseEntity.ok(quotes);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @PostMapping("/scrape")
    @PermitAll
    public ResponseEntity<String> scrapeAndSavePlayers() {
        try {
            long startTime = System.currentTimeMillis();

            // Definir las ligas a scrapear - Map de nombre de liga + URL
            Map<String, String> ligas = new LinkedHashMap<>();
            ligas.put("LaLiga", "https://es.whoscored.com/regions/206/tournaments/4/seasons/10803/stages/24622/playerstatistics/espa%C3%B1a-laliga-2025-2026");
            ligas.put("Premier League", "https://es.whoscored.com/regions/252/tournaments/2/seasons/10743/stages/24533/playerstatistics/inglaterra-premier-league-2025-2026");
            ligas.put("Bundesliga", "https://es.whoscored.com/regions/81/tournaments/3/seasons/10720/stages/24478/playerstatistics/alemania-bundesliga-2025-2026");
            ligas.put("Serie A", "https://es.whoscored.com/regions/108/tournaments/5/seasons/10732/stages/24500/playerstatistics/italia-serie-a-2025-2026");
            ligas.put("Ligue 1", "https://es.whoscored.com/regions/74/tournaments/22/seasons/10792/stages/24609/playerstatistics/francia-ligue-1-2025-2026");

            int totalJugadores = 0;
            int[] totalGuardados = {0}; // Array para poder modificar dentro del lambda
            boolean isFirstLeague = true;

            for (Map.Entry<String, String> liga : ligas.entrySet()) {
                // Callback que guarda los jugadores de cada página inmediatamente
                var jugadores = scraperService.scrapeAllPlayers(
                    liga.getValue(),
                    liga.getKey(),
                    playersPage -> {
                        playerService.saveAllPlayers(playersPage);
                        totalGuardados[0] += playersPage.size();
                    },
                    isFirstLeague  // Solo limpiar en la primera liga
                );

                totalJugadores += jugadores.size();

                // Después de la primera liga, no limpiar más
                isFirstLeague = false;
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;

            String message = String.format(
                "✓ Se scrapearon y guardaron %d jugadores de todas las ligas correctamente%n⏱️ Tiempo total: %d min %d seg",
                totalJugadores,
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
            ligas.put("LaLiga", "https://es.whoscored.com/regions/206/tournaments/4/seasons/10803/stages/24622/playerstatistics/espa%C3%B1a-laliga-2025-2026");
            ligas.put("Premier League", "https://es.whoscored.com/regions/252/tournaments/2/seasons/10743/stages/24533/playerstatistics/inglaterra-premier-league-2025-2026");
            ligas.put("Bundesliga", "https://es.whoscored.com/regions/81/tournaments/3/seasons/10720/stages/24478/playerstatistics/alemania-bundesliga-2025-2026");
            ligas.put("Serie A", "https://es.whoscored.com/regions/108/tournaments/5/seasons/10732/stages/24500/playerstatistics/italia-serie-a-2025-2026");
            ligas.put("Ligue 1", "https://es.whoscored.com/regions/74/tournaments/22/seasons/10792/stages/24609/playerstatistics/francia-ligue-1-2025-2026");

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

            // El método scrapeTeamPlayersByName ahora agrega nuevos y actualiza existentes
            scraperService.scrapeTeamPlayersByName(teamName, league);

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

}
