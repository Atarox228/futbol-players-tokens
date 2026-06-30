package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.modelo.ScoringConfig;
import com.desapp.futbolplayerstokens.modelo.ValuationMode;
import com.desapp.futbolplayerstokens.repository.ScoringConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringConfigServiceImplTest {

    @Mock
    private ScoringConfigRepository repository;

    @InjectMocks
    private ScoringConfigServiceImpl scoringConfigService;

    @Test
    void getActiveMode_defaultsToGeneral_whenNoConfig() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        ValuationMode result = scoringConfigService.getActiveMode();

        assertEquals(ValuationMode.GENERAL, result);
    }

    @Test
    void getActiveMode_returnsPersistedMode() {
        ScoringConfig config = ScoringConfig.builder().id(1L).mode(ValuationMode.POSITION).build();
        when(repository.findById(1L)).thenReturn(Optional.of(config));

        ValuationMode result = scoringConfigService.getActiveMode();

        assertEquals(ValuationMode.POSITION, result);
    }

    @Test
    void setActiveMode_persistsAndReturnsMode() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.save(any(ScoringConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ValuationMode result = scoringConfigService.setActiveMode(ValuationMode.POSITION);

        assertEquals(ValuationMode.POSITION, result);
        verify(repository).save(argThat(config -> config.getMode() == ValuationMode.POSITION));
    }

    @Test
    void setActiveMode_updatesExistingConfig() {
        ScoringConfig existing = ScoringConfig.builder().id(1L).mode(ValuationMode.GENERAL).build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ScoringConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ValuationMode result = scoringConfigService.setActiveMode(ValuationMode.POSITION);

        assertEquals(ValuationMode.POSITION, result);
        assertEquals(ValuationMode.POSITION, existing.getMode());
        verify(repository).save(existing);
    }
}
