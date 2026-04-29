package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;
import com.desapp.futbolplayerstokens.modelo.TeamEnum;
import com.desapp.futbolplayerstokens.service.PlayerOverwriteResult;
import com.desapp.futbolplayerstokens.service.PlayerService;
import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/players")
public class PlayerControllerREST {

    private final PlayerService playerService;
    private final PlayerScraperService scraperService;

    public PlayerControllerREST(PlayerService playerService,
                                PlayerScraperService scraperService) {
        this.playerService = playerService;
        this.scraperService = scraperService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello World";
    }

    @GetMapping("/{id}")
    public PlayerDTO getPlayer(@PathVariable Long id) {
        return playerService.getPlayerById(id);
    }

    @PostMapping("/scrape")
    public ResponseEntity<String> scrapeAndSavePlayers() {
        try {
            long startTime = System.currentTimeMillis();

            System.out.println("\n🔍 Iniciando scraping de todas las ligas...\n");

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
                System.out.println("\n" + "=".repeat(60));
                System.out.println("📍 Scrapeando: " + liga.getKey());
                System.out.println("=".repeat(60) + "\n");

                // Callback que guarda los jugadores de cada página inmediatamente
                var jugadores = scraperService.scrapeAllPlayers(
                    liga.getValue(),
                    liga.getKey(),
                    playersPage -> {
                        System.out.println("💾 Guardando " + playersPage.size() + " jugadores de esta página...");
                        playerService.saveAllPlayers(playersPage);
                        totalGuardados[0] += playersPage.size();
                        System.out.println("📊 Total en BD: " + totalGuardados[0] + " jugadores\n");
                    },
                    isFirstLeague  // Solo limpiar en la primera liga
                );

                totalJugadores += jugadores.size();

                System.out.println("✓ Completado: " + liga.getKey() + " (" + jugadores.size() + " jugadores)\n");

                // Después de la primera liga, no limpiar más
                isFirstLeague = false;
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;

            String message = String.format(
                "✓ Se scrapearon y guardaron %d jugadores de todas las ligas correctamente\n⏱️ Tiempo total: %d min %d seg",
                totalJugadores,
                minutes,
                seconds
            );

            System.out.println("\n" + "=".repeat(60));
            System.out.println(message);
            System.out.println("=".repeat(60) + "\n");

            return ResponseEntity.ok(message);
        } catch (Exception e) {
            System.err.println("❌ Error durante el scraping: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Error: " + e.getMessage());
        }
    }

    @PostMapping("/scrape/new-only")
    public ResponseEntity<String> scrapeAndSaveNewPlayersOnly() {
        try {
            long startTime = System.currentTimeMillis();

            System.out.println("\n🔍 Iniciando scraping de jugadores NUEVOS solamente...\n");

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
                System.out.println("\n" + "=".repeat(60));
                System.out.println("📍 Scrapeando NUEVOS: " + liga.getKey());
                System.out.println("=".repeat(60) + "\n");

                // Callback que guarda solo los jugadores nuevos de cada página
                var jugadoresNuevos = scraperService.scrapeNewPlayersOnly(
                    liga.getValue(),
                    liga.getKey(),
                    playersPage -> {
                        System.out.println("💾 Guardando " + playersPage.size() + " jugadores NUEVOS de esta página...");
                        playerService.saveAllPlayers(playersPage);
                        totalGuardados[0] += playersPage.size();
                        System.out.println("📊 Total NUEVO en BD: " + totalGuardados[0] + " jugadores\n");
                    }
                );

                totalJugadoresNuevos += jugadoresNuevos.size();

                System.out.println("✓ Completado: " + liga.getKey() + " (" + jugadoresNuevos.size() + " jugadores nuevos)\n");
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;

            String message = String.format(
                "✓ Se encontraron y guardaron %d jugadores NUEVOS de todas las ligas\n⏱️ Tiempo total: %d min %d seg",
                totalJugadoresNuevos,
                minutes,
                seconds
            );

            System.out.println("\n" + "=".repeat(60));
            System.out.println(message);
            System.out.println("=".repeat(60) + "\n");

            return ResponseEntity.ok(message);
        } catch (Exception e) {
            System.err.println("❌ Error durante el scraping: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Error: " + e.getMessage());
        }
    }


    @PostMapping("/scrape/team/{id}")
    public ResponseEntity<String> scrapeAndOverwriteTeamPlayers(@PathVariable Integer id) {
        try {
            TeamEnum teamEnum = TeamEnum.fromId(id);
            String teamName = teamEnum.getName();
            String league = teamEnum.getLeague();

            long startTime = System.currentTimeMillis();
            System.out.println("\n🔍 Iniciando scraping de plantilla para: " + teamName + " (" + league + ")");

            // El método scrapeTeamPlayersByName ahora agrega nuevos y actualiza existentes
            List<PlayerDTO> newPlayers = scraperService.scrapeTeamPlayersByName(teamName, league);

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

