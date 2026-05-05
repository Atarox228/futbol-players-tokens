package com.desapp.futbolplayerstokens.scheduler;

import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PlayerScraperScheduler {

    private final PlayerScraperService playerScraperService;

    public PlayerScraperScheduler(PlayerScraperService playerScraperService) {
        this.playerScraperService = playerScraperService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scrapePlayersOnStartupIfDatabaseEmpty() {
        try {
            System.out.println("⏳ Verificando si se necesita scraping inicial de jugadores...");
            playerScraperService.scrapeAllPlayersIfDatabaseEmpty();
        } catch (Exception e) {
            System.err.println("❌ Error en scraping inicial: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * TESTING: Cada minuto (cron = "0 * * * * *")
     * PRODUCCIÓN: Cambiar a "0 0 0 * * *" (cada día a las 00:00)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scrapePlayersDaily() {
        try {
            System.out.println("🔄 Ejecutando scraping diario de jugadores...");
            playerScraperService.scrapeAllPlayersIfDatabaseEmpty();
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
