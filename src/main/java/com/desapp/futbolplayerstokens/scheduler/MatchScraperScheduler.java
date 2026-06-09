package com.desapp.futbolplayerstokens.scheduler;

import com.desapp.futbolplayerstokens.service.MatchScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class MatchScraperScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MatchScraperScheduler.class);

    private final MatchScraperService matchScraperService;
    private final DynamicMatchScheduler dynamicMatchScheduler;

    public MatchScraperScheduler(MatchScraperService matchScraperService, DynamicMatchScheduler dynamicMatchScheduler) {
        this.matchScraperService = matchScraperService;
        this.dynamicMatchScheduler = dynamicMatchScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scrapeOnApplicationStart() {
        logger.info("🚀 Aplicación iniciada. Ejecutando scraping inicial...");
        scrapeMatchesDaily();
    }

    /**
     * TESTING: Cada minuto (cron = "0 * * * * *")
     * PRODUCCIÓN: Cambiar a "0 0 0 * * *" (cada día a las 00:00)
     */
    @Scheduled(cron = "1 0 0 * * *")
    public void scrapeMatchesDaily() {
        try {
            logger.info("🔄 Iniciando scraping de partidos...");
            matchScraperService.scrapeMatchesOfToday();
            logger.info("✅ Scraping completado. Programando schedulers...");
            dynamicMatchScheduler.scheduleMatchesForToday();
            logger.info("✅ Schedulers programados. {} partidos en la cola", dynamicMatchScheduler.getScheduledMatchCount());
        } catch (Exception e) {
            logger.error("❌ Error: {}", e.getMessage());
        }
    }
}
