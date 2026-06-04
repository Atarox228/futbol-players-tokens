package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.MatchDTO;
import com.desapp.futbolplayerstokens.modelo.Match;
import com.desapp.futbolplayerstokens.service.MatchScraperService;
import com.desapp.futbolplayerstokens.service.MatchService;

import jakarta.annotation.security.PermitAll;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@Tag(name = "Matches", description = "Endpoints para gestión de partidos de fútbol")
public class MatchControllerREST {

    private final MatchService matchService;
    private final MatchScraperService matchScraperService;

    public MatchControllerREST(MatchService matchService, MatchScraperService matchScraperService) {
        this.matchService = matchService;
        this.matchScraperService = matchScraperService;
    }

    @GetMapping("/all")
    @Operation(summary = "Obtener todos los partidos", description = "Retorna la lista completa de partidos registrados")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de partidos")
    })
    public ResponseEntity<List<MatchDTO>> getAllMatches() {
        List<Match> matches = matchService.getAllMatches();
        List<MatchDTO> matchDTOs = matches.stream()
            .map(MatchDTO::fromEntity)
            .toList();
        return ResponseEntity.ok(matchDTOs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener partido por ID", description = "Retorna la información detallada de un partido específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Partido encontrado"),
        @ApiResponse(responseCode = "404", description = "Partido no encontrado")
    })
    public ResponseEntity<MatchDTO> getMatchById(
            @Parameter(description = "ID del partido")
            @PathVariable Long id) {
        return matchService.getMatchById(id)
            .map(match -> ResponseEntity.ok(MatchDTO.fromEntity(match)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/team/{teamId}")
    @Operation(summary = "Obtener partidos por equipo", description = "Retorna todos los partidos de un equipo específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de partidos del equipo")
    })
    public ResponseEntity<List<MatchDTO>> getMatchesByTeamId(
            @Parameter(description = "ID del equipo")
            @PathVariable Long teamId) {
        List<Match> matches = matchService.getMatchesByTeamId(teamId);
        List<MatchDTO> matchDTOs = matches.stream()
            .map(MatchDTO::fromEntity)
            .toList();
        return ResponseEntity.ok(matchDTOs);
    }

    @PostMapping
    @Operation(summary = "Crear nuevo partido", description = "Crea un nuevo partido en la base de datos")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Partido creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<MatchDTO> createMatch(@RequestBody MatchDTO matchDTO) {
        Match match = Match.builder()
            .footballDataMatchId(matchDTO.getFootballDataMatchId())
            .team1Id(matchDTO.getTeam1Id())
            .team2Id(matchDTO.getTeam2Id())
            .matchTime(matchDTO.getMatchTime())
            .build();

        Match savedMatch = matchService.createMatch(match);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(MatchDTO.fromEntity(savedMatch));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar partido", description = "Actualiza la información de un partido existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Partido actualizado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Partido no encontrado")
    })
    public ResponseEntity<MatchDTO> updateMatch(
            @Parameter(description = "ID del partido")
            @PathVariable Long id, 
            @RequestBody MatchDTO matchDTO) {
        Match match = Match.builder()
            .footballDataMatchId(matchDTO.getFootballDataMatchId())
            .team1Id(matchDTO.getTeam1Id())
            .team2Id(matchDTO.getTeam2Id())
            .matchTime(matchDTO.getMatchTime())
            .build();

        Match updatedMatch = matchService.updateMatch(id, match);
        return ResponseEntity.ok(MatchDTO.fromEntity(updatedMatch));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar partido", description = "Elimina un partido de la base de datos")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Partido eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Partido no encontrado")
    })
    public ResponseEntity<Void> deleteMatch(
            @Parameter(description = "ID del partido")
            @PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/scrape/today")
    @PermitAll
    @Operation(summary = "Raspar partidos de hoy", description = "Extrae los partidos del día actual de fuentes externas")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scrape completado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error durante el scraping")
    })
    public ResponseEntity<List<MatchDTO>> scrapeMatchesOfToday() {
        List<Match> matches = matchScraperService.scrapeMatchesOfToday();
        List<MatchDTO> matchDTOs = matches.stream()
            .map(MatchDTO::fromEntity)
            .toList();
        return ResponseEntity.ok(matchDTOs);
    }
}
