package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.UpdateModeRequest;
import com.desapp.futbolplayerstokens.controller.dto.UpdateStrategyRequest;
import com.desapp.futbolplayerstokens.modelo.ScoringConfig;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig.StrategyType;
import com.desapp.futbolplayerstokens.modelo.ValuationMode;
import com.desapp.futbolplayerstokens.modelo.QuoteTrigger;
import com.desapp.futbolplayerstokens.service.ActiveStrategyService;
import com.desapp.futbolplayerstokens.service.QuoteService;
import com.desapp.futbolplayerstokens.service.ScoringConfigService;
import com.desapp.futbolplayerstokens.service.StrategyConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/strategies")
@Tag(name = "Strategies", description = "Endpoints para consultar y configurar estrategias de valuación")
public class StrategyConfigController {

    private final ActiveStrategyService activeStrategyService;
    private final StrategyConfigService strategyConfigService;
    private final ScoringConfigService scoringConfigService;
    private final QuoteService quoteService;

    public StrategyConfigController(ActiveStrategyService activeStrategyService,
                                    StrategyConfigService strategyConfigService,
                                    ScoringConfigService scoringConfigService,
                                    QuoteService quoteService) {
        this.activeStrategyService = activeStrategyService;
        this.strategyConfigService = strategyConfigService;
        this.scoringConfigService = scoringConfigService;
        this.quoteService = quoteService;
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

    @GetMapping("/mode")
    @Operation(summary = "Obtener modo de puntuación activo", description = "Retorna el modo de puntuación actual (GENERAL o POSITION)")
    public ResponseEntity<ScoringConfig> getActiveMode() {
        ValuationMode mode = scoringConfigService.getActiveMode();
        ScoringConfig config = ScoringConfig.builder().id(1L).mode(mode).build();
        return ResponseEntity.ok(config);
    }

    @PutMapping("/mode")
    @Operation(summary = "Cambiar modo de puntuación", description = "Cambia el modo de puntuación entre GENERAL (mismas métricas para todos) y POSITION (métricas según posición). Al cambiar el modo se recalculan automáticamente todas las valuaciones.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Modo actualizado y valuaciones recalculadas"),
        @ApiResponse(responseCode = "400", description = "Modo inválido", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public ResponseEntity<ScoringConfig> updateMode(@RequestBody UpdateModeRequest request) {
        if (request.getMode() == null) {
            return ResponseEntity.badRequest().build();
        }
        ValuationMode mode = scoringConfigService.setActiveMode(request.getMode());
        quoteService.recalculateAll(QuoteTrigger.MANUAL);
        ScoringConfig config = ScoringConfig.builder().id(1L).mode(mode).build();
        return ResponseEntity.ok(config);
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
    @Operation(summary = "Actualizar estrategia", description = "Crea una nueva versión de la estrategia con los pesos actualizados. La suma de los pesos debe ser ≤ 1. Al actualizar se recalculan automáticamente todas las valuaciones.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estrategia actualizada y valuaciones recalculadas"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public ResponseEntity<StrategyConfig> updateStrategy(@PathVariable("type") String type,
                                                           @RequestBody UpdateStrategyRequest request) {
        StrategyType strategyType = StrategyType.valueOf(type.toUpperCase());
        StrategyConfig updated = strategyConfigService.update(strategyType, request);
        quoteService.recalculateAll(QuoteTrigger.MANUAL);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{type}/normalized")
    @Operation(summary = "Actualizar estrategia con pesos normalizados", description = "Crea una nueva versión de la estrategia normalizando automáticamente los pesos para que sumen exactamente 1. Al actualizar se recalculan automáticamente todas las valuaciones.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estrategia actualizada con pesos normalizados y valuaciones recalculadas"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public ResponseEntity<StrategyConfig> updateStrategyNormalized(@PathVariable("type") String type,
                                                                     @RequestBody UpdateStrategyRequest request) {
        StrategyType strategyType = StrategyType.valueOf(type.toUpperCase());
        StrategyConfig updated = strategyConfigService.updateNormalized(strategyType, request);
        quoteService.recalculateAll(QuoteTrigger.MANUAL);
        return ResponseEntity.ok(updated);
    }
}
