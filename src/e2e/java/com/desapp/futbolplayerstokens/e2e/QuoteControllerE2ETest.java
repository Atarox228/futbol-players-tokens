package com.desapp.futbolplayerstokens.e2e;

import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Quote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuoteControllerE2ETest extends E2EBaseTest {

    private Player player;

    @BeforeEach
    void seedQuoteData() {
        player = seedPlayer("Pedri", "Barcelona", "LALIGA", "Midfielder", new BigDecimal("94.00000000"), 7, 10);
    }

    @Test
    void getCurrentQuoteReturnsLatestQuote() throws Exception {
        Quote quote = seedQuote(player, new BigDecimal("180.50000000"));

        mockMvc.perform(get("/quotes/player/" + player.getId() + "/current")
                        .header("Authorization", authHeaderValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(quote.getId().intValue()))
                .andExpect(jsonPath("$.playerId").value(player.getId().intValue()))
                .andExpect(jsonPath("$.price").isNumber())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.strategyId").isNumber())
                .andExpect(jsonPath("$.strategyVersion").isNumber())
                .andExpect(jsonPath("$.trigger").value("MANUAL"));
    }

    @Test
    void recalculateQuotesCreatesFreshQuoteAndUpdatesScore() throws Exception {
        mockMvc.perform(post("/quotes/recalculate")
                        .header("Authorization", authHeaderValue())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Recalculation triggered for all players"));

        mockMvc.perform(get("/quotes/player/" + player.getId() + "/current")
                        .header("Authorization", authHeaderValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(player.getId().intValue()));

        Player updatedPlayer = playerRepository.findById(player.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotNull(updatedPlayer.getScore());
        org.junit.jupiter.api.Assertions.assertFalse(quoteRepository.findByPlayerIdOrderByTimestampDesc(player.getId()).isEmpty());
    }
}