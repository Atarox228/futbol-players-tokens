package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.UpdateStrategyRequest;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig.StrategyType;
import com.desapp.futbolplayerstokens.service.ActiveStrategyService;
import com.desapp.futbolplayerstokens.service.StrategyConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/strategies")
@Tag(name = "Strategies", description = "Endpoints para consultar configuraciones de estrategias de valuación")
public class StrategyConfigController {

    private final ActiveStrategyService activeStrategyService;
    private final StrategyConfigService strategyConfigService;

    public StrategyConfigController(ActiveStrategyService activeStrategyService,
                                    StrategyConfigService strategyConfigService) {
        this.activeStrategyService = activeStrategyService;
        this.strategyConfigService = strategyConfigService;
    }

    @GetMapping("/active")
    @Operation(summary = "Obtener estrategia activa", description = "Retorna la configuración de estrategia activa (última versión)")
    public ResponseEntity<StrategyConfig> getActiveStrategy() {
        StrategyConfig config = activeStrategyService.getActiveStrategyConfig();
        return ResponseEntity.ok(config);
    }

    @GetMapping
    @Operation(summary = "Obtener todas las estrategias activas", description = "Retorna la configuración activa de cada tipo de estrategia")
    public ResponseEntity<List<StrategyConfig>> getAllStrategies() {
        return ResponseEntity.ok(strategyConfigService.findAllActive());
    }

    @GetMapping("/{type}")
    @Operation(summary = "Obtener estrategia activa por tipo")
    public ResponseEntity<StrategyConfig> getStrategyByType(@PathVariable("type") String type) {
        StrategyType strategyType = StrategyType.valueOf(type.toUpperCase());
        return ResponseEntity.ok(strategyConfigService.findActiveByType(strategyType));
    }

    @GetMapping("/{type}/history")
    @Operation(summary = "Obtener historial de versiones de una estrategia")
    public ResponseEntity<List<StrategyConfig>> getStrategyHistory(@PathVariable("type") String type) {
        StrategyType strategyType = StrategyType.valueOf(type.toUpperCase());
        return ResponseEntity.ok(strategyConfigService.getHistoryByType(strategyType));
    }

    @PutMapping("/{type}")
    @Operation(summary = "Actualizar estrategia", description = "Crea una nueva versión de la estrategia con los pesos actualizados")
    public ResponseEntity<StrategyConfig> updateStrategy(@PathVariable("type") String type,
                                                          @RequestBody UpdateStrategyRequest request) {
        StrategyType strategyType = StrategyType.valueOf(type.toUpperCase());
        return ResponseEntity.ok(strategyConfigService.update(strategyType, request));
    }
}
