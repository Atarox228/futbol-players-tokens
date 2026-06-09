package com.desapp.futbolplayerstokens.e2e;

import com.desapp.futbolplayerstokens.modelo.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlayerControllerE2ETest extends E2EBaseTest {

    private Player forward;
    private Player midfielder;

    @BeforeEach
    void seedPlayers() {
        forward = seedPlayer("Lionel Messi", "Inter Miami", "MLS", "Forward", new BigDecimal("98.50000000"), 8, 10);
        midfielder = seedPlayer("Luka Modric", "Real Madrid", "LALIGA", "Midfielder", new BigDecimal("95.25000000"), 6, 10);
    }

    @Test
    void getPlayersWithoutFiltersReturnsAllPlayers() throws Exception {
        mockMvc.perform(get("/players")
                        .header("Authorization", authHeaderValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].team").isString())
                .andExpect(jsonPath("$[0].league").isString())
                .andExpect(jsonPath("$[0].position").isString())
                .andExpect(jsonPath("$[0].score").isNumber());
    }

    @Test
    void getPlayersWithFiltersReturnsOnlyMatchingPlayers() throws Exception {
        mockMvc.perform(get("/players")
                        .header("Authorization", authHeaderValue())
                        .param("league", "LALIGA")
                        .param("team", "Real Madrid")
                        .param("position", "Midfielder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(midfielder.getId().intValue()))
                .andExpect(jsonPath("$[0].name").value("Luka Modric"))
                .andExpect(jsonPath("$[0].league").value("LALIGA"))
                .andExpect(jsonPath("$[0].position").value("Midfielder"));
    }

    @Test
    void getPlayerByIdReturnsDetailedBody() throws Exception {
        mockMvc.perform(get("/players/" + forward.getId())
                        .header("Authorization", authHeaderValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(forward.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Lionel Messi"))
                .andExpect(jsonPath("$.team").value("Inter Miami"))
                .andExpect(jsonPath("$.league").value("MLS"))
                .andExpect(jsonPath("$.position").value("Forward"))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.appearances").isNumber())
                .andExpect(jsonPath("$.minutes").isNumber());
    }

    @Test
    void getRankingReturnsOrderedPlayers() throws Exception {
        mockMvc.perform(get("/players/ranking")
                        .header("Authorization", authHeaderValue())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].playerId").value(forward.getId().toString()))
                .andExpect(jsonPath("$[0].playerName").value("Lionel Messi"))
                .andExpect(jsonPath("$[0].score").isNumber())
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[1].playerId").value(midfielder.getId().toString()));
    }
}