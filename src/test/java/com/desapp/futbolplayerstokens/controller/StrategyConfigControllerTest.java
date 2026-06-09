package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.UpdateStrategyRequest;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.service.ActiveStrategyService;
import com.desapp.futbolplayerstokens.service.StrategyConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StrategyConfigControllerTest {

    @Mock
    private ActiveStrategyService activeStrategyService;

    @Mock
    private StrategyConfigService strategyConfigService;

    @InjectMocks
    private StrategyConfigController strategyConfigController;

    @Test
    void getActiveStrategy_returnsConfig() {
        StrategyConfig config = StrategyConfig.builder()
                .type(StrategyConfig.StrategyType.GENERAL).version(3).build();
        when(activeStrategyService.getActiveStrategyConfig()).thenReturn(config);

        ResponseEntity<StrategyConfig> result = strategyConfigController.getActiveStrategy();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(StrategyConfig.StrategyType.GENERAL, result.getBody().getType());
    }

    @Test
    void getAllStrategies_returnsList() {
        when(strategyConfigService.findAllActive())
                .thenReturn(List.of(
                        StrategyConfig.builder().type(StrategyConfig.StrategyType.GENERAL).build(),
                        StrategyConfig.builder().type(StrategyConfig.StrategyType.FORWARD).build()
                ));

        ResponseEntity<List<StrategyConfig>> result = strategyConfigController.getAllStrategies();

        assertEquals(2, result.getBody().size());
    }

    @Test
    void getStrategyByType_returnsConfig() {
        StrategyConfig config = StrategyConfig.builder()
                .type(StrategyConfig.StrategyType.FORWARD).version(2).build();
        when(strategyConfigService.findActiveByType(StrategyConfig.StrategyType.FORWARD))
                .thenReturn(config);

        ResponseEntity<StrategyConfig> result = strategyConfigController.getStrategyByType("FORWARD");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(StrategyConfig.StrategyType.FORWARD, result.getBody().getType());
    }

    @Test
    void getStrategyByType_invalidType_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> strategyConfigController.getStrategyByType("INVALID"));
    }

    @Test
    void getStrategyHistory_returnsHistory() {
        when(strategyConfigService.getHistoryByType(StrategyConfig.StrategyType.GENERAL))
                .thenReturn(List.of(
                        StrategyConfig.builder().version(1).build(),
                        StrategyConfig.builder().version(2).build()
                ));

        ResponseEntity<List<StrategyConfig>> result = strategyConfigController.getStrategyHistory("GENERAL");

        assertEquals(2, result.getBody().size());
    }

    @Test
    void getStrategyHistory_invalidType_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> strategyConfigController.getStrategyHistory("NONEXISTENT"));
    }

    @Test
    void updateStrategy_returnsUpdatedConfig() {
        UpdateStrategyRequest request = new UpdateStrategyRequest();
        request.setValorBase(new BigDecimal("5"));
        request.setFactorEscala(new BigDecimal("15"));
        request.setWeights(Map.of("goals", new BigDecimal("0.5")));

        StrategyConfig updated = StrategyConfig.builder()
                .type(StrategyConfig.StrategyType.DEFENDER).version(3).build();
        when(strategyConfigService.update(StrategyConfig.StrategyType.DEFENDER, request))
                .thenReturn(updated);

        ResponseEntity<StrategyConfig> result = strategyConfigController.updateStrategy("DEFENDER", request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3, result.getBody().getVersion());
    }

    @Test
    void updateStrategy_invalidType_throwsException() {
        UpdateStrategyRequest request = new UpdateStrategyRequest();
        assertThrows(IllegalArgumentException.class,
                () -> strategyConfigController.updateStrategy("BADTYPE", request));
    }

    @Test
    void updateStrategy_midfielder_success() {
        UpdateStrategyRequest request = new UpdateStrategyRequest();
        request.setWeights(Map.of("passes", BigDecimal.ONE));

        StrategyConfig config = StrategyConfig.builder()
                .type(StrategyConfig.StrategyType.MIDFIELDER).version(1).build();
        when(strategyConfigService.update(StrategyConfig.StrategyType.MIDFIELDER, request))
                .thenReturn(config);

        ResponseEntity<StrategyConfig> result = strategyConfigController.updateStrategy("MIDFIELDER", request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
