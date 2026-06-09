package com.desapp.futbolplayerstokens.e2e;

import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.repository.OrderRepository;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser
@Transactional
class PlayerControllerE2ETest extends AbstractE2ETest {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    private Long playerId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        playerRepository.deleteAll();
        entityManager.flush();
        playerId = playerRepository.save(Player.builder()
                .name("E2E Player One")
                .team("Team A")
                .league("LaLiga")
                .position("Forward")
                .score(new BigDecimal("85.50"))
                .build()).getId();
        playerRepository.save(Player.builder()
                .name("E2E Player Two")
                .team("Team B")
                .league("Premier League")
                .position("Midfielder")
                .score(new BigDecimal("75.00"))
                .build());
        entityManager.flush();
    }

    @Test
    void hello() throws Exception {
        mockMvc.perform(get("/players/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World"));
    }

    @Test
    void getPlayers_returnsAll() throws Exception {
        mockMvc.perform(get("/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getPlayers_withLeagueFilter() throws Exception {
        mockMvc.perform(get("/players?league=LaLiga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("E2E Player One"));
    }

    @Test
    void getPlayerById() throws Exception {
        mockMvc.perform(get("/players/{id}", playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("E2E Player One"));
    }

    @Test
    void getPlayerById_notFound() throws Exception {
        mockMvc.perform(get("/players/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRanking() throws Exception {
        mockMvc.perform(get("/players/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
