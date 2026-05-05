package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.MatchDTO;
import com.desapp.futbolplayerstokens.modelo.Match;
import com.desapp.futbolplayerstokens.service.MatchScraperService;
import com.desapp.futbolplayerstokens.service.MatchService;

import jakarta.annotation.security.PermitAll;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchControllerREST {

    private final MatchService matchService;
    private final MatchScraperService matchScraperService;

    public MatchControllerREST(MatchService matchService, MatchScraperService matchScraperService) {
        this.matchService = matchService;
        this.matchScraperService = matchScraperService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<MatchDTO>> getAllMatches() {
        List<Match> matches = matchService.getAllMatches();
        List<MatchDTO> matchDTOs = matches.stream()
            .map(MatchDTO::fromEntity)
            .toList();
        return ResponseEntity.ok(matchDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDTO> getMatchById(@PathVariable Long id) {
        return matchService.getMatchById(id)
            .map(match -> ResponseEntity.ok(MatchDTO.fromEntity(match)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<MatchDTO>> getMatchesByTeamId(@PathVariable Long teamId) {
        List<Match> matches = matchService.getMatchesByTeamId(teamId);
        List<MatchDTO> matchDTOs = matches.stream()
            .map(MatchDTO::fromEntity)
            .toList();
        return ResponseEntity.ok(matchDTOs);
    }

    @PostMapping
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
    public ResponseEntity<MatchDTO> updateMatch(@PathVariable Long id, @RequestBody MatchDTO matchDTO) {
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
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/scrape/today")
    @PermitAll
    public ResponseEntity<List<MatchDTO>> scrapeMatchesOfToday() {
        List<Match> matches = matchScraperService.scrapeMatchesOfToday();
        List<MatchDTO> matchDTOs = matches.stream()
            .map(MatchDTO::fromEntity)
            .toList();
        return ResponseEntity.ok(matchDTOs);
    }
}
