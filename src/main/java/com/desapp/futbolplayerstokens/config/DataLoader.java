package com.desapp.futbolplayerstokens.config;

import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.OrderRepository;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.service.PlayerScraperService;
import com.desapp.futbolplayerstokens.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Profile("!test")
public class DataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final UserService userService;
    private final PlayerScraperService scraperService;
    private final PlayerRepository playerRepository;
    private final StrategyConfigRepository strategyConfigRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public DataLoader(UserService userService,
                      PlayerScraperService scraperService,
                      PlayerRepository playerRepository,
                      StrategyConfigRepository strategyConfigRepository,
                      OrderRepository orderRepository,
                      UserRepository userRepository) {
        this.userService = userService;
        this.scraperService = scraperService;
        this.playerRepository = playerRepository;
        this.strategyConfigRepository = strategyConfigRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) throws Exception {
        try {
            scraperService.scrapeAllPlayersIfDatabaseEmpty();
        } catch (Exception e) {
            log.warn("Scraper failed, loading mock players: {}", e.getMessage());
            loadMockPlayers();
        }

        createSuperuserIfMissing();
        createTestUsersIfMissing();
        createDefaultStrategyIfMissing();
    }

    private void loadMockPlayers() {
        Player p1 = Player.builder().name("Player PL 1").team("Team PL").league("Premier League").position("Forward").availableTokens(100).totalTokens(100).build();
        Player p2 = Player.builder().name("Player LL 1").team("Team LL").league("LaLiga").position("Midfielder").availableTokens(100).totalTokens(100).build();
        Player p3 = Player.builder().name("Player BL 1").team("Team BL").league("Bundesliga").position("Defender").availableTokens(100).totalTokens(100).build();
        Player p4 = Player.builder().name("Player SA 1").team("Team SA").league("Serie A").position("Forward").availableTokens(100).totalTokens(100).build();
        Player p5 = Player.builder().name("Player L1 1").team("Team L1").league("Ligue 1").position("Goalkeeper").availableTokens(100).totalTokens(100).build();
        playerRepository.save(p1);
        playerRepository.save(p2);
        playerRepository.save(p3);
        playerRepository.save(p4);
        playerRepository.save(p5);
    }

    private void createSuperuserIfMissing() {
        userRepository.findByUsername("superuser").orElseGet(() -> {
            User u = User.builder()
                    .username("superuser")
                    .password(new BCryptPasswordEncoder().encode("superpass"))
                    .email("superuser@example.com")
                    .role(User.Role.SUPERUSER)
                    .balance(BigDecimal.ZERO)
                    .build();
            return userRepository.save(u);
        });
    }

    private void createTestUsersIfMissing() {
        for (int i = 1; i <= 4; i++) {
            String uname = "user" + i;
            if (userRepository.findByUsername(uname).isEmpty()) {
                userService.registerUser(uname, "password", uname + "@example.com");
                // ensure balance is set to 1000 by default on creation
            }
        }
    }

    private void createDefaultStrategyIfMissing() {
        if (strategyConfigRepository.findTopByOrderByVersionDesc().isEmpty()) {
            StrategyConfig cfg = StrategyConfig.builder()
                    .valorBase(new BigDecimal("1"))
                    .factorEscala(new BigDecimal("10"))
                    .version(1)
                    .weights(defaultStrategyWeights())
                    .build();
            strategyConfigRepository.save(cfg);
        }
    }

    private Map<String, BigDecimal> defaultStrategyWeights() {
        Map<String, BigDecimal> weights = new LinkedHashMap<>();

        addGeneralStrategyWeights(weights);
        addPositionStrategyWeights(weights);

        return weights;
    }

    private void addGeneralStrategyWeights(Map<String, BigDecimal> weights) {
        weights.put("goals", BigDecimal.ZERO);
        weights.put("assists", BigDecimal.ZERO);
        weights.put("keyPasses", BigDecimal.ZERO);
        weights.put("dribbles", BigDecimal.ZERO);
        weights.put("tackles", BigDecimal.ZERO);
        weights.put("minutes", new BigDecimal("0.50"));
        weights.put("rating", new BigDecimal("0.50"));
        weights.put("yellowCards", new BigDecimal("0.20"));
        weights.put("redCards", new BigDecimal("0.40"));
    }

    private void addPositionStrategyWeights(Map<String, BigDecimal> weights) {
        weights.put("gk_clears", BigDecimal.ONE);
        weights.put("gk_blocks", BigDecimal.ZERO);
        weights.put("gk_interceptions", BigDecimal.ZERO);
        weights.put("gk_rating", BigDecimal.ZERO);
        weights.put("gk_redCards", BigDecimal.ZERO);

        weights.put("df_interceptions", new BigDecimal("0.35"));
        weights.put("df_tackles", new BigDecimal("0.35"));
        weights.put("df_ownGoals", new BigDecimal("0.15"));
        weights.put("df_faults", new BigDecimal("0.15"));
        weights.put("df_clears", BigDecimal.ZERO);
        weights.put("df_blocks", BigDecimal.ZERO);
        weights.put("df_rating", BigDecimal.ZERO);
        weights.put("df_redCards", BigDecimal.ZERO);
        weights.put("df_yellowCards", BigDecimal.ZERO);

        weights.put("mf_keyPasses", new BigDecimal("0.50"));
        weights.put("mf_passAccuracy", new BigDecimal("0.50"));
        weights.put("mf_assists", BigDecimal.ZERO);
        weights.put("mf_dribbles", BigDecimal.ZERO);
        weights.put("mf_tackles", BigDecimal.ZERO);
        weights.put("mf_rating", BigDecimal.ZERO);
        weights.put("mf_yellowCards", BigDecimal.ZERO);
        weights.put("mf_redCards", BigDecimal.ZERO);

        weights.put("fw_ownGoals", new BigDecimal("0.50"));
        weights.put("fw_shots", new BigDecimal("0.50"));
        weights.put("fw_goals", BigDecimal.ZERO);
        weights.put("fw_dribbles", BigDecimal.ZERO);
        weights.put("fw_assists", BigDecimal.ZERO);
        weights.put("fw_keyPasses", BigDecimal.ZERO);
        weights.put("fw_redCards", BigDecimal.ZERO);
        weights.put("fw_yellowCards", BigDecimal.ZERO);
    }
}

