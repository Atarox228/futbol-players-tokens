package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.UpdateStrategyRequest;
import com.desapp.futbolplayerstokens.exception.ResourceNotFoundException;
import com.desapp.futbolplayerstokens.exception.ValidationException;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StrategyConfigServiceImplTest {

    @Mock
    private StrategyConfigRepository repository;

    @InjectMocks
    private StrategyConfigServiceImpl strategyConfigService;

    @Test
    void findAllActive_returnsList() {
        when(repository.findAllActive()).thenReturn(List.of(
                StrategyConfig.builder().type(StrategyConfig.StrategyType.GENERAL).build()
        ));

        List<StrategyConfig> result = strategyConfigService.findAllActive();

        assertEquals(1, result.size());
    }

    @Test
    void findAllActive_empty() {
        when(repository.findAllActive()).thenReturn(List.of());

        List<StrategyConfig> result = strategyConfigService.findAllActive();

        assertTrue(result.isEmpty());
    }

    @Test
    void findActiveByType_found() {
        StrategyConfig config = StrategyConfig.builder()
                .type(StrategyConfig.StrategyType.FORWARD).version(2).build();
        when(repository.findTopByTypeOrderByVersionDesc(StrategyConfig.StrategyType.FORWARD))
                .thenReturn(Optional.of(config));

        StrategyConfig result = strategyConfigService.findActiveByType(StrategyConfig.StrategyType.FORWARD);

        assertEquals(StrategyConfig.StrategyType.FORWARD, result.getType());
        assertEquals(2, result.getVersion());
    }

    @Test
    void findActiveByType_notFound_throwsException() {
        when(repository.findTopByTypeOrderByVersionDesc(StrategyConfig.StrategyType.GOALKEEPER))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> strategyConfigService.findActiveByType(StrategyConfig.StrategyType.GOALKEEPER));
    }

    @Test
    void getHistoryByType_returnsHistory() {
        when(repository.findByTypeOrderByVersionDesc(StrategyConfig.StrategyType.GENERAL))
                .thenReturn(List.of(
                        StrategyConfig.builder().version(2).build(),
                        StrategyConfig.builder().version(1).build()
                ));

        List<StrategyConfig> result = strategyConfigService.getHistoryByType(StrategyConfig.StrategyType.GENERAL);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getVersion());
    }

    @Test
    void getHistoryByType_empty_throwsException() {
        when(repository.findByTypeOrderByVersionDesc(StrategyConfig.StrategyType.GENERAL))
                .thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> strategyConfigService.getHistoryByType(StrategyConfig.StrategyType.GENERAL));
    }

    @Test
    void update_createsNewVersion() {
        UpdateStrategyRequest request = new UpdateStrategyRequest();
        request.setValorBase(new BigDecimal("2"));
        request.setFactorEscala(new BigDecimal("12"));
        request.setWeights(Map.of("goals", new BigDecimal("0.3"), "assists", new BigDecimal("0.2")));

        when(repository.findTopByTypeOrderByVersionDesc(StrategyConfig.StrategyType.GENERAL))
                .thenReturn(Optional.of(StrategyConfig.builder().version(3).build()));

        StrategyConfig saved = StrategyConfig.builder()
                .type(StrategyConfig.StrategyType.GENERAL)
                .version(4)
                .valorBase(new BigDecimal("2"))
                .factorEscala(new BigDecimal("12"))
                .weights(Map.of("goals", new BigDecimal("0.3"), "assists", new BigDecimal("0.2")))
                .build();
        when(repository.save(any(StrategyConfig.class))).thenReturn(saved);

        StrategyConfig result = strategyConfigService.update(StrategyConfig.StrategyType.GENERAL, request);

        assertEquals(4, result.getVersion());
        assertEquals(new BigDecimal("2"), result.getValorBase());
        verify(repository).save(argThat(cfg -> cfg.getVersion() == 4));
    }

    @Test
    void update_firstVersion_whenNoPreviousExists() {
        UpdateStrategyRequest request = new UpdateStrategyRequest();
        request.setValorBase(BigDecimal.ONE);
        request.setFactorEscala(BigDecimal.TEN);
        request.setWeights(Map.of("goals", BigDecimal.ONE));

        when(repository.findTopByTypeOrderByVersionDesc(StrategyConfig.StrategyType.GENERAL))
                .thenReturn(Optional.empty());

        when(repository.save(any(StrategyConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StrategyConfig result = strategyConfigService.update(StrategyConfig.StrategyType.GENERAL, request);

        assertEquals(1, result.getVersion());
    }

    @Test
    void update_nullWeights_throwsException() {
        UpdateStrategyRequest request = new UpdateStrategyRequest();
        request.setWeights(null);

        assertThrows(ValidationException.class,
                () -> strategyConfigService.update(StrategyConfig.StrategyType.GENERAL, request));
        verify(repository, never()).save(any());
    }

    @Test
    void update_emptyWeights_throwsException() {
        UpdateStrategyRequest request = new UpdateStrategyRequest();
        request.setWeights(Map.of());

        assertThrows(ValidationException.class,
                () -> strategyConfigService.update(StrategyConfig.StrategyType.GENERAL, request));
    }

    @Test
    void update_negativeWeight_throwsException() {
        UpdateStrategyRequest request = new UpdateStrategyRequest();
        request.setWeights(Map.of("goals", new BigDecimal("-0.1")));

        assertThrows(ValidationException.class,
                () -> strategyConfigService.update(StrategyConfig.StrategyType.GENERAL, request));
    }

    @Test
    void update_nullWeightValue_throwsException() {
        Map<String, BigDecimal> weights = new java.util.HashMap<>();
        weights.put("goals", null);
        UpdateStrategyRequest request = new UpdateStrategyRequest();
        request.setWeights(weights);

        assertThrows(ValidationException.class,
                () -> strategyConfigService.update(StrategyConfig.StrategyType.GENERAL, request));
    }

    @Test
    void update_weightsSumExceedsOne_throwsException() {
        UpdateStrategyRequest request = new UpdateStrategyRequest();
        request.setWeights(Map.of("goals", new BigDecimal("0.6"), "assists", new BigDecimal("0.5")));

        assertThrows(ValidationException.class,
                () -> strategyConfigService.update(StrategyConfig.StrategyType.GENERAL, request));
    }

    @Test
    void update_weightsSumExactlyOne_allowed() {
        UpdateStrategyRequest request = new UpdateStrategyRequest();
        request.setValorBase(BigDecimal.ONE);
        request.setFactorEscala(BigDecimal.TEN);
        request.setWeights(Map.of("goals", new BigDecimal("0.7"), "assists", new BigDecimal("0.3")));

        when(repository.findTopByTypeOrderByVersionDesc(StrategyConfig.StrategyType.GENERAL))
                .thenReturn(Optional.of(StrategyConfig.builder().version(1).build()));
        when(repository.save(any(StrategyConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(
                () -> strategyConfigService.update(StrategyConfig.StrategyType.GENERAL, request));
    }
}
