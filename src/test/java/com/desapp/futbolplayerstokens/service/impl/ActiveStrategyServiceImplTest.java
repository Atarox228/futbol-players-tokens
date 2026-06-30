package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.repository.StrategyConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActiveStrategyServiceImplTest {

    @Mock
    private StrategyConfigRepository strategyConfigRepository;

    @InjectMocks
    private ActiveStrategyServiceImpl activeStrategyService;

    @Test
    void getActiveStrategyConfig_found() {
        StrategyConfig config = StrategyConfig.builder()
                .type(StrategyConfig.StrategyType.GENERAL).version(3).build();
        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyConfig.StrategyType.GENERAL))
                .thenReturn(Optional.of(config));

        StrategyConfig result = activeStrategyService.getActiveStrategyConfig();

        assertEquals(StrategyConfig.StrategyType.GENERAL, result.getType());
        assertEquals(3, result.getVersion());
    }

    @Test
    void getActiveStrategyConfig_notFound_throwsException() {
        when(strategyConfigRepository.findTopByTypeOrderByVersionDesc(StrategyConfig.StrategyType.GENERAL))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> activeStrategyService.getActiveStrategyConfig());
    }
}
