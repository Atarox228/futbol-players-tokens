package com.desapp.futbolplayerstokens.service;

public interface ValuationStrategyRouter {
    Strategy resolve(String strategyKey);
}

