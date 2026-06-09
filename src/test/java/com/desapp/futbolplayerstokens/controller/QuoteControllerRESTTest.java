package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.service.QuoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteControllerRESTTest {

    @Mock
    private QuoteService quoteService;

    @InjectMocks
    private QuoteControllerREST quoteController;

    @Test
    void recalculateAll_returnsAccepted() {
        doNothing().when(quoteService).recalculateAll(QuoteTrigger.MANUAL);

        ResponseEntity<String> result = quoteController.recalculateAll();

        assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
        assertEquals("Recalculation triggered for all players", result.getBody());
        verify(quoteService).recalculateAll(QuoteTrigger.MANUAL);
    }

    @Test
    void getCurrentQuote_returnsQuote() {

        QuoteDTO dto = QuoteDTO.builder()
                .id(1L)
                .playerId(10L)
                .price(new BigDecimal("85.50"))
                .timestamp(LocalDateTime.of(2026, Month.JUNE, 9, 12, 0))
                .trigger("MANUAL")
                .build();
        when(quoteService.getCurrentQuote(10L)).thenReturn(dto);

        ResponseEntity<QuoteDTO> result = quoteController.getCurrentQuote(10L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getId());
        assertEquals(new BigDecimal("85.50"), result.getBody().getPrice());
    }

    @Test
    void getCurrentQuote_quotesDifferentPlayers() {
        QuoteDTO q1 = QuoteDTO.builder().id(1L).playerId(10L).price(new BigDecimal("50")).build();
        QuoteDTO q2 = QuoteDTO.builder().id(2L).playerId(11L).price(new BigDecimal("100")).build();
        when(quoteService.getCurrentQuote(10L)).thenReturn(q1);
        when(quoteService.getCurrentQuote(11L)).thenReturn(q2);

        ResponseEntity<QuoteDTO> r1 = quoteController.getCurrentQuote(10L);
        ResponseEntity<QuoteDTO> r2 = quoteController.getCurrentQuote(11L);

        assertEquals(new BigDecimal("50"), r1.getBody().getPrice());
        assertEquals(new BigDecimal("100"), r2.getBody().getPrice());
    }
}
