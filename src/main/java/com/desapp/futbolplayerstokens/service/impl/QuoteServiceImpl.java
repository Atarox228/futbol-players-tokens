package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.exception.ConfigurationException;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.Quote;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig.StrategyType;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.modelo.ValuationMode;
import com.desapp.futbolplayerstokens.service.QuoteService;
import com.desapp.futbolplayerstokens.service.ScoringConfigService;
import com.desapp.futbolplayerstokens.service.ValuationService;
import com.desapp.futbolplayerstokens.service.impl.ScoreByPositionStrategy;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class QuoteServiceImpl implements QuoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteServiceImpl.class);
    private static final String ESTRATEGIAINACTIVA = "No active strategy config";
    private static final String JUGADOR_NOENCONTRADO = "Player not found with id: ";

    private static final String SUPERUSER_USERNAME = "superuser";

    private final QuoteRepository quoteRepository;
    private final PlayerRepository playerRepository;
    private final StrategyConfigRepository strategyConfigRepository;
    private final ValuationService valuationService;
    private final TransactionTemplate transactionTemplate;
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final ScoringConfigService scoringConfigService;
    private final Timer recalculateTimer;


    public QuoteServiceImpl(QuoteRepository quoteRepository,
                            PlayerRepository playerRepository,
                            StrategyConfigRepository strategyConfigRepository,
                            ValuationService valuationService,
                            TransactionTemplate transactionTemplate,
                            UserRepository userRepository,
                            PortfolioRepository portfolioRepository,
                            ScoringConfigService scoringConfigService,
                            MeterRegistry meterRegistry) {
        this.quoteRepository = quoteRepository;
        this.playerRepository = playerRepository;
        this.strategyConfigRepository = strategyConfigRepository;
        this.valuationService = valuationService;
        this.transactionTemplate = transactionTemplate;
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.scoringConfigService = scoringConfigService;
        this.recalculateTimer = Timer.builder("quotes.recalculate.duration")
                .description("Time taken to recalculate all quotes")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    @Override
    public List<QuoteDTO> getQuotesByPlayerId(Long playerId) {
        List<Quote> quotes = quoteRepository.findByPlayerIdOrderByTimestampDesc(playerId);
        if (quotes.isEmpty()) {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new RuntimeException(JUGADOR_NOENCONTRADO + playerId));

            StrategyConfig config = resolveConfigForPlayer(player);
            Quote q = recalculateSingle(player, config, QuoteTrigger.MANUAL);
            return List.of(QuoteDTO.toDTO(q));
        }

        return quotes.stream().map(QuoteDTO::toDTO).toList();
    }

    @Override
    public QuoteDTO getCurrentQuote(Long playerId) {
        Optional<Quote> optional = quoteRepository.findTopByPlayerIdOrderByTimestampDesc(playerId);
        if (optional.isPresent()) {
            return QuoteDTO.toDTO(optional.get());
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException(JUGADOR_NOENCONTRADO + playerId));

        StrategyConfig config = resolveConfigForPlayer(player);
        Quote q = recalculateSingle(player, config, QuoteTrigger.MANUAL);
        return QuoteDTO.toDTO(q);
    }

    private StrategyConfig resolveConfigForPlayer(Player player) {
        StrategyConfig general = strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.GENERAL)
                .orElseThrow(() -> new ConfigurationException(ESTRATEGIAINACTIVA));
        ValuationMode mode = scoringConfigService.getActiveMode();
        if (mode == ValuationMode.GENERAL) return general;
        StrategyType type = ScoreByPositionStrategy.resolveType(player.getPosition());
        if (type == StrategyType.GENERAL) return general;
        return strategyConfigRepository.findTopByTypeOrderByVersionDesc(type).orElse(general);
    }

    @Override
    @Transactional
    public void recalculateAll(QuoteTrigger trigger) {
        long start = System.nanoTime();
        transactionTemplate.executeWithoutResult(status -> doRecalculateAll(trigger));
        recalculateTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
    }

    private void doRecalculateAll(QuoteTrigger trigger) {
        StrategyConfig general = strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.GENERAL)
                .orElseThrow(() -> new ConfigurationException(ESTRATEGIAINACTIVA));
        ValuationMode mode = scoringConfigService.getActiveMode();

        List<Player> players = playerRepository.findAll();
        int total = 0;
        for (Player player : players) {
            StrategyConfig config;
            if (mode == ValuationMode.GENERAL) {
                config = general;
            } else {
                StrategyType type = ScoreByPositionStrategy.resolveType(player.getPosition());
                config = type == StrategyType.GENERAL
                        ? general
                        : strategyConfigRepository.findTopByTypeOrderByVersionDesc(type).orElse(general);
            }
            recalculateSingle(player, config, trigger);
            total++;
        }

        seedSuperuserPortfolioForMissingPlayers();

        LOGGER.info("Recalculation finished. Total players processed: {}", total);
    }

    @Override
    @Transactional
    public void recalculatePlayers(List<Long> playerIds, QuoteTrigger trigger) {
        if (playerIds.isEmpty()) return;

        StrategyConfig general = strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyType.GENERAL)
                .orElseThrow(() -> new ConfigurationException(ESTRATEGIAINACTIVA));
        ValuationMode mode = scoringConfigService.getActiveMode();

        for (Long playerId : playerIds) {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new RuntimeException(JUGADOR_NOENCONTRADO + playerId));
            StrategyConfig config;
            if (mode == ValuationMode.GENERAL) {
                config = general;
            } else {
                StrategyType type = ScoreByPositionStrategy.resolveType(player.getPosition());
                config = type == StrategyType.GENERAL
                        ? general
                        : strategyConfigRepository.findTopByTypeOrderByVersionDesc(type).orElse(general);
            }
            recalculateSingle(player, config, trigger);
        }

        seedSuperuserPortfolioForMissingPlayers();

        LOGGER.info("Recalculation finished for {} players", playerIds.size());
    }

    private void seedSuperuserPortfolioForMissingPlayers() {
        User superuser = userRepository.findByUsername(SUPERUSER_USERNAME).orElse(null);
        if (superuser == null) {
            LOGGER.warn("Superuser not found, skipping portfolio seeding");
            return;
        }
        int seeded = 0;
        for (Player player : playerRepository.findAll()) {
            if (portfolioRepository.findByUserAndPlayer(superuser, player).isEmpty()) {
                Portfolio portfolio = Portfolio.builder()
                        .user(superuser)
                        .player(player)
                        .tokenQty(player.getTotalTokens())
                        .avgBuyPrice(BigDecimal.ZERO)
                        .build();
                portfolioRepository.save(portfolio);
                seeded++;
            }
        }
        if (seeded > 0) {
            LOGGER.info("Seeded superuser portfolio for {} new players", seeded);
        }
    }

    private Quote recalculateSingle(Player player, StrategyConfig config, QuoteTrigger trigger) {
        ValuationMode mode = scoringConfigService.getActiveMode();
        String strategyKey = mode == ValuationMode.GENERAL ? null : "POSITION";
        ValuationResult result = valuationService.evaluatePlayer(player.getId(), config.getId(), strategyKey);

        Quote q = Quote.builder()
                .player(player)
                .price(result.getPrice())
                .timestamp(LocalDateTime.now())
                .strategyId(config.getId())
                .strategyVersion(config.getVersion())
                .trigger(trigger)
                .build();

        return quoteRepository.save(q);
    }
}



