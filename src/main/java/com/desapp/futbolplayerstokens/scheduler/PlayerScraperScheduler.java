package com.desapp.futbolplayerstokens.scheduler;

import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PlayerScraperScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PlayerScraperScheduler.class);

    private final PlayerScraperService playerScraperService;

    public PlayerScraperScheduler(PlayerScraperService playerScraperService) {
        this.playerScraperService = playerScraperService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scrapePlayersOnStartupIfDatabaseEmpty() {
        try {
            logger.info("⏳ Verificando si se necesita scraping inicial de jugadores...");
            playerScraperService.scrapeAllPlayersIfDatabaseEmpty();
        } catch (Exception e) {
            logger.error("❌ Error en scraping inicial: {}", e.getMessage());
        }
    }

    /**
     * TESTING: Cada minuto (cron = "0 * * * * *")
     * PRODUCCIÓN: Cambiar a "0 0 0 * * *" (cada día a las 00:00)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scrapePlayersDaily() {
        try {
            logger.info("🔄 Ejecutando scraping diario de jugadores...");
            playerScraperService.scrapeAllPlayersIfDatabaseEmpty();
        } catch (Exception e) {
            logger.error("❌ Error: {}", e.getMessage());
        }
    }
}
