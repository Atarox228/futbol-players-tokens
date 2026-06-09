package com.desapp.futbolplayerstokens.e2e;

import com.desapp.futbolplayerstokens.modelo.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerE2ETest extends E2EBaseTest {

    private Player player;

    @BeforeEach
    void seedUserData() {
        player = seedPlayer("Vinicius Jr", "Real Madrid", "LALIGA", "Forward", new BigDecimal("96.00000000"), 4, 10);
        seedQuote(player, new BigDecimal("150.00000000"));
        seedPortfolio(authenticatedUser, player, 3, new BigDecimal("120.00000000"));
    }

    @Test
    void getBalanceReturnsUserSnapshot() throws Exception {
        mockMvc.perform(get("/users/" + authenticatedUser.getId() + "/balance")
                        .header("Authorization", authHeaderValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(authenticatedUser.getId().intValue()))
                .andExpect(jsonPath("$.username").value(DEFAULT_USERNAME))
                .andExpect(jsonPath("$.balance").isNumber());
    }

    @Test
    void getPortfolioReturnsPortfolioItems() throws Exception {
        mockMvc.perform(get("/users/" + authenticatedUser.getId() + "/portfolio")
                        .header("Authorization", authHeaderValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].playerId").value(player.getId().intValue()))
                .andExpect(jsonPath("$[0].playerName").value("Vinicius Jr"))
                .andExpect(jsonPath("$[0].tokenQty").value(3))
                .andExpect(jsonPath("$[0].avgBuyPrice").isNumber())
                .andExpect(jsonPath("$[0].currentPrice").isNumber())
                .andExpect(jsonPath("$[0].profitLoss").isNumber());
    }
}