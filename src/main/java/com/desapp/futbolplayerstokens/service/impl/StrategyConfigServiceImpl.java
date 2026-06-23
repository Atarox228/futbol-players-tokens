package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.UpdateStrategyRequest;
import com.desapp.futbolplayerstokens.exception.ResourceNotFoundException;
import com.desapp.futbolplayerstokens.exception.ValidationException;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig.StrategyType;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import com.desapp.futbolplayerstokens.service.StrategyConfigService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StrategyConfigServiceImpl implements StrategyConfigService {

    private static final BigDecimal ONE = BigDecimal.ONE;

    private final StrategyConfigRepository repository;

    public StrategyConfigServiceImpl(StrategyConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StrategyConfig> findAllActive() {
        return repository.findAllActive();
    }

    @Override
    public StrategyConfig findActiveByType(StrategyType type) {
        return repository.findTopByTypeOrderByVersionDesc(type)
                .orElseThrow(() -> new ResourceNotFoundException("No strategy config found for type: " + type));
    }

    @Override
    public List<StrategyConfig> getHistoryByType(StrategyType type) {
        List<StrategyConfig> history = repository.findByTypeOrderByVersionDesc(type);
        if (history.isEmpty()) {
            throw new ResourceNotFoundException("No strategy config found for type: " + type);
        }
        return history;
    }

    @Override
    public StrategyConfig update(StrategyType type, UpdateStrategyRequest request) {
        validateWeights(request.getWeights());

        int nextVersion = repository.findTopByTypeOrderByVersionDesc(type)
                .map(cfg -> cfg.getVersion() + 1)
                .orElse(1);

        StrategyConfig config = StrategyConfig.builder()
                .type(type)
                .valorBase(request.getValorBase())
                .factorEscala(request.getFactorEscala())
                .version(nextVersion)
                .weights(request.getWeights())
                .build();

        return repository.save(config);
    }

    @Override
    public StrategyConfig updateNormalized(StrategyType type, UpdateStrategyRequest request) {
        Map<String, BigDecimal> normalized = normalizeWeights(request.getWeights());

        UpdateStrategyRequest normalizedRequest = new UpdateStrategyRequest(
                request.getValorBase(),
                request.getFactorEscala(),
                normalized
        );

        return update(type, normalizedRequest);
    }

    private Map<String, BigDecimal> normalizeWeights(Map<String, BigDecimal> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new ValidationException("Weights cannot be empty");
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal w : weights.values()) {
            if (w == null || w.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("All weights must be non-negative");
            }
            sum = sum.add(w);
        }

        if (sum.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("Sum of weights must be greater than 0");
        }

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            BigDecimal normalized = entry.getValue().divide(sum, 8, RoundingMode.HALF_UP);
            result.put(entry.getKey(), normalized);
        }

        return result;
    }

    private void validateWeights(Map<String, BigDecimal> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new ValidationException("Weights cannot be empty");
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal w : weights.values()) {
            if (w == null || w.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("All weights must be non-negative");
            }
            sum = sum.add(w);
        }
        if (sum.compareTo(ONE) > 0) {
            throw new ValidationException("Sum of all weights (" + sum + ") must not exceed 1");
        }
    }
}
