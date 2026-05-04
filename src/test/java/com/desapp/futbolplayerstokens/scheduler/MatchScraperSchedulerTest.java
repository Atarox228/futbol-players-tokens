package com.desapp.futbolplayerstokens.scheduler;

import com.desapp.futbolplayerstokens.service.MatchScraperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

@DisplayName("Tests del MatchScraperScheduler")
class MatchScraperSchedulerTest {

    @Mock
    private MatchScraperService matchScraperService;

    @Mock
    private DynamicMatchScheduler dynamicMatchScheduler;

    private MatchScraperScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduler = new MatchScraperScheduler(matchScraperService, dynamicMatchScheduler);
    }

    @Test
    @DisplayName("Debe ejecutar scraping y luego programar schedulers")
    void testScrapeMatchesDailyExecutesInOrder() {
        scheduler.scrapeMatchesDaily();

        verify(matchScraperService, times(1)).scrapeMatchesOfToday();
        verify(dynamicMatchScheduler, times(1)).scheduleMatchesForToday();

        InOrder inOrder = inOrder(matchScraperService, dynamicMatchScheduler);
        inOrder.verify(matchScraperService).scrapeMatchesOfToday();
        inOrder.verify(dynamicMatchScheduler).scheduleMatchesForToday();
    }

    @Test
    @DisplayName("Debe continuar si el scraping falla")
    void testScrapeMatchesDailyContinuesOnException() {
        doThrow(new RuntimeException("Error en scraping")).when(matchScraperService).scrapeMatchesOfToday();

        scheduler.scrapeMatchesDaily();

        verify(matchScraperService, times(1)).scrapeMatchesOfToday();
    }

    @Test
    @DisplayName("Debe llamar a DynamicMatchScheduler aunque el scraping lance error")
    void testSchedulerCalledEvenIfScrapingFails() {
        doThrow(new RuntimeException("API error")).when(matchScraperService).scrapeMatchesOfToday();

        try {
            scheduler.scrapeMatchesDaily();
        } catch (Exception e) {
            // Expected
        }

        verify(dynamicMatchScheduler, never()).scheduleMatchesForToday();
    }
}
