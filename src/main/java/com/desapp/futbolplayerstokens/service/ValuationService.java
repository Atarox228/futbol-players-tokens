package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;

public interface ValuationService {

    default ValuationResult evaluatePlayer(Long playerId, Long strategyConfigId) {
        return evaluatePlayer(playerId, strategyConfigId, null);
    }

    ValuationResult evaluatePlayer(Long playerId, Long strategyConfigId, String strategyKey);
}

