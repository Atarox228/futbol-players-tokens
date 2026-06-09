package com.desapp.futbolplayerstokens.e2e;

import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(username = "e2e-user-controller")
@Transactional
class UserControllerE2ETest extends AbstractE2ETest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    private Long userId;
    private Long playerId;

    @BeforeEach
    void setUp() {
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
        playerRepository.deleteAll();

        userId = userRepository.save(User.builder()
                .username("e2e-user-controller")
                .email("e2e-uc@example.com")
                .password("password")
                .role(User.Role.USER)
                .balance(new BigDecimal("500"))
                .build()).getId();

        playerId = playerRepository.save(Player.builder()
                .name("E2E Portfolio Player")
                .team("Team C")
                .league("Serie A")
                .position("Defender")
                .build()).getId();
    }

    @Test
    void getBalance() throws Exception {
        mockMvc.perform(get("/users/{id}/balance", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.username").value("e2e-user-controller"))
                .andExpect(jsonPath("$.balance").value(500));
    }

    @Test
    void getPortfolio_empty() throws Exception {
        mockMvc.perform(get("/users/{id}/portfolio", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void getPortfolio_withItems() throws Exception {
        portfolioRepository.save(Portfolio.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .player(playerRepository.findById(playerId).orElseThrow())
                .tokenQty(10)
                .avgBuyPrice(new BigDecimal("50"))
                .build());

        mockMvc.perform(get("/users/{id}/portfolio", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].tokenQty").value(10));
    }

    @Test
    void getTransactions_empty() throws Exception {
        mockMvc.perform(get("/users/{id}/transactions", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
