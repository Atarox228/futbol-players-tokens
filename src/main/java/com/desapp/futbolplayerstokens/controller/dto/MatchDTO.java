package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Match;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
    @Schema(description = "Fecha y hora del partido", example = "2026-06-01T20:30:00")
    private LocalDateTime matchTime;

    public static MatchDTO fromEntity(Match match) {
        return MatchDTO.builder()
            .id(match.getId())
            .footballDataMatchId(match.getFootballDataMatchId())
            .team1Id(match.getTeam1Id())
            .team2Id(match.getTeam2Id())
            .matchTime(match.getMatchTime())
            .build();
    }
}
