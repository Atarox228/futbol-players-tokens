package com.desapp.futbolplayerstokens.e2e;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDetailDTO;
import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.Quote;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig.StrategyType;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.OrderRepository;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@Import(E2EBaseTest.E2ETestOverrides.class)
public abstract class E2EBaseTest {

    protected static final String DEFAULT_USERNAME = "e2e_user";
    protected static final String DEFAULT_PASSWORD = "Password123!";
    protected static final String DEFAULT_EMAIL = "e2e_user@example.com";

    @Autowired
    protected WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PlayerRepository playerRepository;

    @Autowired
    protected PortfolioRepository portfolioRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected QuoteRepository quoteRepository;

    @Autowired
    protected StrategyConfigRepository strategyConfigRepository;

    protected User authenticatedUser;
    protected String authToken;

    @BeforeEach
    void baseSetUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        clearData();
        seedCommonData();
        authToken = loginAndGetToken(DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    @AfterEach
    void baseTearDown() {
        clearData();
    }

    protected User seedUser(String username, String email, String rawPassword, BigDecimal balance) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(User.Role.USER)
                .balance(balance)
                .build();
        return userRepository.save(user);
    }

    protected Player seedPlayer(String name,
                                String team,
                                String league,
                                String position,
                                BigDecimal score,
                                int availableTokens,
                                int totalTokens) {
        Player player = Player.builder()
                .name(name)
                .team(team)
                .league(league)
                .position(position)
                .altPosition(position)
                .rating(7.5)
                .appearances(10)
                .minutes(900)
                .goals(4)
                .assists(2)
                .shotsOnTarget(8.0)
                .keyPasses(12.0)
                .dribbles(6.0)
                .tackles(5.0)
                .interceptions(3.0)
                .blocks(1.0)
                .clears(2.0)
                .yellowCards(1)
                .redCards(0)
                .playerOfTheMatch(0)
                .availableTokens(availableTokens)
                .totalTokens(totalTokens)
                .score(score)
                .build();
        return playerRepository.save(player);
    }

    protected Portfolio seedPortfolio(User user, Player player, int tokenQty, BigDecimal avgBuyPrice) {
        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .player(player)
                .tokenQty(tokenQty)
                .avgBuyPrice(avgBuyPrice)
                .build();
        return portfolioRepository.save(portfolio);
    }

    protected Quote seedQuote(Player player, BigDecimal price) {
        Quote quote = Quote.builder()
                .player(player)
                .price(price)
                .timestamp(LocalDateTime.now().minusMinutes(1))
                .strategyId(1L)
                .strategyVersion(1)
                .trigger(QuoteTrigger.MANUAL)
                .build();
        return quoteRepository.save(quote);
    }

    protected StrategyConfig seedGeneralStrategyConfig() {
        StrategyConfig config = StrategyConfig.builder()
                .valorBase(new BigDecimal("100.00000000"))
                .factorEscala(new BigDecimal("50.00000000"))
                .type(StrategyType.GENERAL)
                .version(1)
                .weights(new HashMap<>())
                .build();
        return strategyConfigRepository.save(config);
    }

    protected Order seedOrder(User user,
                              Player player,
                              Order.OrderType type,
                              int quantity,
                              BigDecimal priceAtOrder,
                              String idempotencyKey,
                              Order.OrderStatus status,
                              int remainingQuantity) {
        Order order = Order.builder()
                .user(user)
                .player(player)
                .type(type)
                .quantity(quantity)
                .priceAtOrder(priceAtOrder)
                .total(priceAtOrder.multiply(BigDecimal.valueOf(quantity)))
                .idempotencyKey(idempotencyKey)
                .status(status)
                .remainingQuantity(remainingQuantity)
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build();
        return orderRepository.save(order);
    }

    protected String loginAndGetToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.path("token").asText();
    }

    protected String authHeaderValue() {
        return "Bearer " + authToken;
    }

    private void seedCommonData() {
        authenticatedUser = seedUser(DEFAULT_USERNAME, DEFAULT_EMAIL, DEFAULT_PASSWORD, new BigDecimal("1000.00000000"));
        seedGeneralStrategyConfig();
    }

    private void clearData() {
        quoteRepository.deleteAll();
        orderRepository.deleteAll();
        portfolioRepository.deleteAll();
        strategyConfigRepository.deleteAll();
        playerRepository.deleteAll();
        userRepository.deleteAll();
    }

    @TestConfiguration
    static class E2ETestOverrides {

        @Bean
        @Primary
        PlayerScraperService playerScraperServiceStub() {
            return new PlayerScraperService() {
                @Override
                public List<PlayerDetailDTO> scrapeAllPlayers(String url, String league, java.util.function.Consumer<List<PlayerDetailDTO>> onPageComplete) {
                    return Collections.emptyList();
                }

                @Override
                public List<PlayerDetailDTO> scrapeAllPlayers(String url, String league, java.util.function.Consumer<List<PlayerDetailDTO>> onPageComplete, boolean clearTable) {
                    return Collections.emptyList();
                }

                @Override
                public List<PlayerDetailDTO> scrapeTeamPlayersByName(String teamName, String league) {
                    return Collections.emptyList();
                }

                @Override
                public List<PlayerDetailDTO> scrapeLeaguePlayersByStarterTeam(String starterTeam, String league) {
                    return Collections.emptyList();
                }

                @Override
                public List<PlayerDetailDTO> scrapeNewPlayersOnly(String url, String league, java.util.function.Consumer<List<PlayerDetailDTO>> onPageComplete) {
                    return Collections.emptyList();
                }

                @Override
                public void scrapeAllPlayersIfDatabaseEmpty() {
                }

                @Override
                public void scrapeAllPlayersForce() {
                }
            };
        }
    }
}