package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.exception.ConfigurationException;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Quote;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.service.QuoteService;
import com.desapp.futbolplayerstokens.service.ValuationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuoteServiceImpl implements QuoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteServiceImpl.class);

    private final QuoteRepository quoteRepository;
    private final PlayerRepository playerRepository;
    private final StrategyConfigRepository strategyConfigRepository;
    private final ValuationService valuationService;
    private final TransactionTemplate transactionTemplate;

    private final String estrategiaInactiva = "No active strategy config";

    public QuoteServiceImpl(QuoteRepository quoteRepository,
                            PlayerRepository playerRepository,
                            StrategyConfigRepository strategyConfigRepository,
                            ValuationService valuationService,
                            TransactionTemplate transactionTemplate) {
        this.quoteRepository = quoteRepository;
        this.playerRepository = playerRepository;
        this.strategyConfigRepository = strategyConfigRepository;
        this.valuationService = valuationService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public List<QuoteDTO> getQuotesByPlayerId(Long playerId) {
        List<Quote> quotes = quoteRepository.findByPlayerIdOrderByTimestampDesc(playerId);
        if (quotes.isEmpty()) {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new RuntimeException("Player not found with id: " + playerId));

            StrategyConfig active = strategyConfigRepository.findTopByOrderByVersionDesc()
                    .orElseThrow(() -> new ConfigurationException(estrategiaInactiva));

            Quote q = recalculateSingle(player, active, QuoteTrigger.MANUAL);
            return List.of(QuoteDTO.toDTO(q));
        }

        return quotes.stream().map(QuoteDTO::toDTO).collect(Collectors.toList());
    }

    @Override
    public QuoteDTO getCurrentQuote(Long playerId) {
        Optional<Quote> optional = quoteRepository.findTopByPlayerIdOrderByTimestampDesc(playerId);
        if (optional.isPresent()) {
            return QuoteDTO.toDTO(optional.get());
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found with id: " + playerId));

        StrategyConfig active = strategyConfigRepository.findTopByOrderByVersionDesc()
                .orElseThrow(() -> new ConfigurationException(estrategiaInactiva));

        Quote q = recalculateSingle(player, active, QuoteTrigger.MANUAL);
        return QuoteDTO.toDTO(q);
    }

    @Override
    @Transactional
    public void recalculateAll(QuoteTrigger trigger) {
        if (trigger == QuoteTrigger.MANUAL) {
            CompletableFuture.runAsync(() -> {
                try {
                    transactionTemplate.executeWithoutResult(status -> doRecalculateAll(trigger));
                } catch (Exception e) {
                    LOGGER.error("Error while running manual recalculation", e);
                }
            });
            LOGGER.info("Manual quote recalculation queued for all players");
            return;
        }

        transactionTemplate.executeWithoutResult(status -> doRecalculateAll(trigger));
    }

    private void doRecalculateAll(QuoteTrigger trigger) {
        StrategyConfig active = strategyConfigRepository.findTopByOrderByVersionDesc()
                .orElseThrow(() -> new ConfigurationException(estrategiaInactiva));

        List<Player> players = playerRepository.findAll();
        int total = 0;
        for (Player player : players) {
            recalculateSingle(player, active, trigger);
            total++;
        }

        LOGGER.info("Recalculation finished. Total players processed: {}", total);
    }

    private Quote recalculateSingle(Player player, StrategyConfig config, QuoteTrigger trigger) {
        ValuationResult result = valuationService.evaluatePlayer(player.getId(), config.getId(), null);

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



