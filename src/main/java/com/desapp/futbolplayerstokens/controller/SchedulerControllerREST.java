package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.scheduler.DynamicMatchScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/scheduler")
@Tag(name = "Scheduler", description = "Endpoints para gestión del programador de partidos")
public class SchedulerControllerREST {

    private final DynamicMatchScheduler dynamicMatchScheduler;

    public SchedulerControllerREST(DynamicMatchScheduler dynamicMatchScheduler) {
        this.dynamicMatchScheduler = dynamicMatchScheduler;
    }

    @GetMapping("/status")
    @Operation(summary = "Obtener estado del scheduler", description = "Retorna el estado actual del programador de partidos")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado del scheduler")
    })
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        Map<String, Object> response = new HashMap<>();
        int count = dynamicMatchScheduler.getScheduledMatchCount();
        response.put("scheduledMatches", count);
        response.put("status", count > 0 ? "ACTIVE" : "IDLE");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/matches")
    @Operation(summary = "Obtener partidos programados", description = "Retorna la lista de todos los partidos programados")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de partidos programados")
    })
    public ResponseEntity<Map<String, Object>> getScheduledMatches() {
        Map<String, Object> response = new HashMap<>();
        List<DynamicMatchScheduler.MatchScheduleInfo> matches = dynamicMatchScheduler.getAllScheduledMatches();
        response.put("total", matches.size());
        response.put("matches", matches);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test/schedule-all")
    @Operation(summary = "Programar todos los partidos (Testing)", description = "Programa todos los partidos para propósitos de testing")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Partidos programados exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error durante la programación")
    })
    public ResponseEntity<Map<String, Object>> scheduleAllForTesting() {
        Map<String, Object> response = new HashMap<>();
        try {
            dynamicMatchScheduler.scheduleAllMatchesForTesting();
            int count = dynamicMatchScheduler.getScheduledMatchCount();
            response.put("success", true);
            response.put("scheduledMatches", count);
            response.put("message", count + " partidos programados para testing");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
