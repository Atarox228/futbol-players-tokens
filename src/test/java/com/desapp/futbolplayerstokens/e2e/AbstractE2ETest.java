package com.desapp.futbolplayerstokens.e2e;

import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Tag("e2e")
public abstract class AbstractE2ETest {

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private StrategyConfigRepository strategyConfigRepository;

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    private void seedStrategyConfigs() {
        if (strategyConfigRepository.count() > 0) return;
        seedStrategy(StrategyConfig.StrategyType.GENERAL, Map.of(
                "goals", new BigDecimal("0.25"),
                "assists", new BigDecimal("0.15"),
                "rating", new BigDecimal("0.20"),
                "minutes", BigDecimal.ZERO,
                "keyPasses", new BigDecimal("0.10"),
                "dribbles", new BigDecimal("0.10"),
                "tackles", new BigDecimal("0.10"),
                "yellowCards", new BigDecimal("0.05"),
                "redCards", new BigDecimal("0.05")
        ));
        seedStrategy(StrategyConfig.StrategyType.FORWARD, Map.of(
                "goals", new BigDecimal("0.35"),
                "ownGoals", BigDecimal.ZERO,
                "shots", new BigDecimal("0.20"),
                "dribbles", new BigDecimal("0.20"),
                "assists", new BigDecimal("0.15"),
                "keyPasses", new BigDecimal("0.10"),
                "redCards", new BigDecimal("0.10"),
                "yellowCards", new BigDecimal("0.05")
        ));
        seedStrategy(StrategyConfig.StrategyType.MIDFIELDER, Map.of(
                "keyPasses", new BigDecimal("0.30"),
                "passAccuracy", BigDecimal.ZERO,
                "assists", new BigDecimal("0.25"),
                "dribbles", new BigDecimal("0.20"),
                "tackles", new BigDecimal("0.15"),
                "rating", new BigDecimal("0.10"),
                "yellowCards", new BigDecimal("0.05"),
                "redCards", new BigDecimal("0.10")
        ));
        seedStrategy(StrategyConfig.StrategyType.DEFENDER, Map.of(
                "tackles", new BigDecimal("0.30"),
                "interceptions", new BigDecimal("0.25"),
                "clears", new BigDecimal("0.20"),
                "blocks", new BigDecimal("0.15"),
                "rating", new BigDecimal("0.10"),
                "redCards", new BigDecimal("0.10"),
                "yellowCards", new BigDecimal("0.05"),
                "ownGoals", BigDecimal.ZERO,
                "faults", BigDecimal.ZERO
        ));
        seedStrategy(StrategyConfig.StrategyType.GOALKEEPER, Map.of(
                "clears", new BigDecimal("0.40"),
                "blocks", new BigDecimal("0.30"),
                "interceptions", new BigDecimal("0.20"),
                "rating", new BigDecimal("0.10"),
                "redCards", new BigDecimal("0.10")
        ));
    }

    private void seedStrategy(StrategyConfig.StrategyType type, Map<String, BigDecimal> weights) {
        strategyConfigRepository.save(StrategyConfig.builder()
                .type(type)
                .valorBase(new BigDecimal("1"))
                .factorEscala(new BigDecimal("10"))
                .version(1)
                .weights(new LinkedHashMap<>(weights))
                .build());
    }
}
