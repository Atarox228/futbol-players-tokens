package com.desapp.futbolplayerstokens.scheduler;

import com.desapp.futbolplayerstokens.service.MatchScraperService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MatchScraperScheduler {

    private final MatchScraperService matchScraperService;
    private final DynamicMatchScheduler dynamicMatchScheduler;

    public MatchScraperScheduler(MatchScraperService matchScraperService, DynamicMatchScheduler dynamicMatchScheduler) {
        this.matchScraperService = matchScraperService;
        this.dynamicMatchScheduler = dynamicMatchScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        new Thread(() -> {
            try {
                Thread.sleep(60000);
                System.out.println("⚡ Ejecutando scraping de prueba 1 minuto después del startup...");
                scrapeMatchesDaily();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * TESTING: Cada minuto (cron = "0 * * * * *")
     * PRODUCCIÓN: Cambiar a "0 0 0 * * *" (cada día a las 00:00)
     */
    @Scheduled(cron = "0 49 19 * * *")
    public void scrapeMatchesDaily() {
        try {
            System.out.println("🔄 Iniciando scraping de partidos...");
            matchScraperService.scrapeMatchesOfToday();
            System.out.println("✅ Scraping completado. Programando schedulers...");
            dynamicMatchScheduler.scheduleMatchesForToday();
            System.out.println("✅ Schedulers programados. " + dynamicMatchScheduler.getScheduledMatchCount() + " partidos en la cola");
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
