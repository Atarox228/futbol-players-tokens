package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Match;
import com.desapp.futbolplayerstokens.modelo.TeamEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "MatchDTO", description = "DTO que representa un partido de fútbol")
public class MatchDTO {
    @Schema(description = "ID único del partido", example = "1")
    private Long id;
    @Schema(description = "ID del partido en Football-Data API", example = "400000001")
    private Long footballDataMatchId;
    @Schema(description = "ID del primer equipo", example = "1")
    private Long team1Id;
    @Schema(description = "ID del segundo equipo", example = "2")
    private Long team2Id;
    @Schema(description = "Nombre del primer equipo", example = "Barcelona")
    private String team1Name;
    @Schema(description = "Nombre del segundo equipo", example = "Real Madrid")
    private String team2Name;
    @Schema(description = "Estado del partido (TIMED, IN_PLAY, FINISHED, etc.)", example = "TIMED")
    private String status;
    @Schema(description = "Fecha y hora del partido", example = "2026-06-01T20:30:00")
    private LocalDateTime matchTime;

    public static MatchDTO fromEntity(Match match) {
        String team1Name = Arrays.stream(TeamEnum.values())
            .filter(t -> t.getId() == match.getTeam1Id().intValue())
            .findFirst()
            .map(TeamEnum::getName)
            .orElse(null);
        String team2Name = Arrays.stream(TeamEnum.values())
            .filter(t -> t.getId() == match.getTeam2Id().intValue())
            .findFirst()
            .map(TeamEnum::getName)
            .orElse(null);
        return MatchDTO.builder()
            .id(match.getId())
            .footballDataMatchId(match.getFootballDataMatchId())
            .team1Id(match.getTeam1Id())
            .team2Id(match.getTeam2Id())
            .team1Name(team1Name)
            .team2Name(team2Name)
            .status(match.getStatus())
            .matchTime(match.getMatchTime())
            .build();
    }
}
