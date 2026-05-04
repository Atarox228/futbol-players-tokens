package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Match;
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
public class MatchDTO {
    private Long id;
    private Long footballDataMatchId;
    private Long team1Id;
    private Long team2Id;
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
