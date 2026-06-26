package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.modelo.ValuationMode;

public interface ScoringConfigService {
    ValuationMode getActiveMode();
    ValuationMode setActiveMode(ValuationMode mode);
}
