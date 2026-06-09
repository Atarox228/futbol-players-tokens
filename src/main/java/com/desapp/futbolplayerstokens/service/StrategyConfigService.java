package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.UpdateStrategyRequest;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig.StrategyType;

import java.util.List;

public interface StrategyConfigService {
    List<StrategyConfig> findAllActive();

    StrategyConfig findActiveByType(StrategyType type);

    StrategyConfig update(StrategyType type, UpdateStrategyRequest request);

    List<StrategyConfig> getHistoryByType(StrategyType type);
}
