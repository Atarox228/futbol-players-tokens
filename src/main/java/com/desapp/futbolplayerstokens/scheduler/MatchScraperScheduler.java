package com.desapp.futbolplayerstokens.scheduler;

import com.desapp.futbolplayerstokens.service.MatchScraperService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MatchScraperScheduler {

    private final MatchScraperService matchScraperService;

    public MatchScraperScheduler(MatchScraperService matchScraperService) {
        this.matchScraperService = matchScraperService;
    }

    /**
     * TESTING: Cada minuto (cron = "0 * * * * *")
     * PRODUCCIÓN: Cambiar a "0 0 0 * * *" (cada día a las 00:00)
     */
    @Scheduled(cron = "0 52 12 * * *")
    public void scrapeMatchesDaily() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("⏰ [" + timestamp + "] 📡 Scrapeando partidos...");

        try {
            matchScraperService.scrapeMatchesOfToday();
            System.out.println("✓ Scrape OK\n");
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage() + "\n");
        }
    }
}
