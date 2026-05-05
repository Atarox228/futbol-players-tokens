package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.ValuationResult;

public interface ValuationService {
    ValuationResult evaluatePlayer(Long playerId, Long strategyConfigId);
}

