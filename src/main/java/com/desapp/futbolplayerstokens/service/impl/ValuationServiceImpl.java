package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.ValuationContext;
import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.service.Strategy;
import com.desapp.futbolplayerstokens.service.ValuationStrategyRouter;
import com.desapp.futbolplayerstokens.service.ValuationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValuationServiceImpl implements ValuationService {

    private final PlayerRepository playerRepository;
    private final StrategyConfigRepository strategyConfigRepository;
    private final ValuationStrategyRouter valuationStrategyRouter;

    public ValuationServiceImpl(PlayerRepository playerRepository,
                                StrategyConfigRepository strategyConfigRepository,
                                ValuationStrategyRouter valuationStrategyRouter) {
        this.playerRepository = playerRepository;
        this.strategyConfigRepository = strategyConfigRepository;
        this.valuationStrategyRouter = valuationStrategyRouter;
    }

    @Override
    @Transactional
    public ValuationResult evaluatePlayer(Long playerId, Long strategyConfigId, String strategyKey) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found with id: " + playerId));

        StrategyConfig strategyConfig = strategyConfigRepository.findById(strategyConfigId)
                .orElseThrow(() -> new RuntimeException("StrategyConfig not found with id: " + strategyConfigId));

        ValuationContext valuationContext = ValuationContext.builder()
                .player(player)
                .strategyConfig(strategyConfig)
                .build();

        Strategy strategy = valuationStrategyRouter.resolve(strategyKey);
        ValuationResult valuationResult = strategy.evaluate(valuationContext);
        int updatedRows = playerRepository.updateScoreById(playerId, valuationResult.getPrice());
        if (updatedRows == 0) {
            throw new RuntimeException("Player score could not be updated for id: " + playerId);
        }

        return valuationResult;
    }
}

