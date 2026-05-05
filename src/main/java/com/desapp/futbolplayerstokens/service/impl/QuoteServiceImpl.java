package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Quote;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.service.QuoteService;
import com.desapp.futbolplayerstokens.service.ValuationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuoteServiceImpl implements QuoteService {

    private final QuoteRepository quoteRepository;
    private final ValuationService valuationService;
    private final StrategyConfigRepository strategyConfigRepository;
    private final PlayerRepository playerRepository;

    public QuoteServiceImpl(QuoteRepository quoteRepository,
                            ValuationService valuationService,
                            StrategyConfigRepository strategyConfigRepository,
                            PlayerRepository playerRepository) {
        this.quoteRepository = quoteRepository;
        this.valuationService = valuationService;
        this.strategyConfigRepository = strategyConfigRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    @Transactional
    public List<QuoteDTO> getQuotesByPlayerId(Long playerId) {
        List<Quote> quotes = quoteRepository.findByPlayerIdOrderByTimestampDesc(playerId);
        if (quotes.isEmpty()) {
            // Verify player exists
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new RuntimeException("Player not found with id: " + playerId));

            // compute a first quote using the latest strategy config
            StrategyConfig strategyConfig = strategyConfigRepository.findAll().stream()
                    .max((a, b) -> a.getVersion().compareTo(b.getVersion()))
                    .orElseThrow(() -> new RuntimeException("No strategy config available to calculate quote"));

            ValuationResult result = valuationService.evaluatePlayer(playerId, strategyConfig.getId());
            Quote q = Quote.builder()
                    .player(player)
                    .price(result.getPrice())
                    .timestamp(LocalDateTime.now())
                    .strategyId(result.getStrategyId())
                    .strategyVersion(result.getStrategyVersion())
                    .trigger(QuoteTrigger.MANUAL)
                    .build();

            q = quoteRepository.save(q);
            quotes = List.of(q);
        }

        return quotes.stream()
                .map(QuoteDTO::toDTO)
                .collect(Collectors.toList());
    }
}



