package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.service.ActiveStrategyService;
import org.springframework.stereotype.Service;

@Service
public class ActiveStrategyServiceImpl implements ActiveStrategyService {

    private final StrategyConfigRepository strategyConfigRepository;

    public ActiveStrategyServiceImpl(StrategyConfigRepository strategyConfigRepository) {
        this.strategyConfigRepository = strategyConfigRepository;
    }

    @Override
    public StrategyConfig getActiveStrategyConfig() {
        return strategyConfigRepository.findTopByOrderByVersionDesc()
                .orElseThrow(() -> new RuntimeException("No active strategy config found"));
    }
}

