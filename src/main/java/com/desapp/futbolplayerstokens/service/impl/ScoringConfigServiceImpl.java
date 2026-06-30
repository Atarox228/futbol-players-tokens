package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.modelo.ScoringConfig;
import com.desapp.futbolplayerstokens.modelo.ValuationMode;
import com.desapp.futbolplayerstokens.repository.ScoringConfigRepository;
import com.desapp.futbolplayerstokens.service.ScoringConfigService;
import org.springframework.stereotype.Service;

@Service
public class ScoringConfigServiceImpl implements ScoringConfigService {

    private static final Long CONFIG_ID = 1L;

    private final ScoringConfigRepository repository;

    public ScoringConfigServiceImpl(ScoringConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public ValuationMode getActiveMode() {
        return repository.findById(CONFIG_ID)
                .map(ScoringConfig::getMode)
                .orElse(ValuationMode.GENERAL);
    }

    @Override
    public ValuationMode setActiveMode(ValuationMode mode) {
        ScoringConfig config = repository.findById(CONFIG_ID)
                .orElse(ScoringConfig.builder()
                        .id(CONFIG_ID)
                        .mode(mode)
                        .build());
        config.setMode(mode);
        repository.save(config);
        return mode;
    }
}
