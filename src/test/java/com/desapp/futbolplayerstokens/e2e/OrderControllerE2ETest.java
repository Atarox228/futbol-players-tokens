package com.desapp.futbolplayerstokens.e2e;

import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(username = "e2e-order-user")
@Transactional
class OrderControllerE2ETest extends AbstractE2ETest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    private Long playerId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        playerRepository.deleteAll();

        userRepository.save(com.desapp.futbolplayerstokens.modelo.User.builder()
                .username("e2e-order-user")
                .email("e2e-order@example.com")
                .password("password")
                .role(com.desapp.futbolplayerstokens.modelo.User.Role.USER)
                .balance(new BigDecimal("10000"))
                .build());

        playerId = playerRepository.save(Player.builder()
                .name("E2E Order Player")
                .team("Team D")
                .league("Bundesliga")
                .position("Forward")
                .score(new BigDecimal("80.00"))
                .availableTokens(100)
                .totalTokens(100)
                .build()).getId();
    }

    @Test
    void buyOrder() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "playerId", playerId,
                "quantity", 5,
                "idempotencyKey", "e2e-buy-" + System.currentTimeMillis(),
                "maxPrice", 100
        ));
        mockMvc.perform(post("/orders/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("BUY"))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void transactions() throws Exception {
        mockMvc.perform(get("/orders/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void orderBook() throws Exception {
        mockMvc.perform(get("/orders/book"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void pendingOrders() throws Exception {
        mockMvc.perform(get("/orders/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
