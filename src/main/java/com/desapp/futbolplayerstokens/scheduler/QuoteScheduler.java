package com.desapp.futbolplayerstokens.scheduler;

import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.service.QuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class QuoteScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteScheduler.class);

    private final QuoteService quoteService;

    public QuoteScheduler(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Scheduled(cron = "0 0 0 * * MON")
    public void scheduledRecalculation() {
        try {
            quoteService.recalculateAll(QuoteTrigger.SCHEDULED);
        } catch (Exception e) {
            LOGGER.error("Error while running scheduled recalculation", e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        LOGGER.info("Quote scheduler ready. Manual recalculation available at POST /quotes/recalculate");
    }
}

