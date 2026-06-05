package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Player;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(name = "PlayerRankingDTO", description = "DTO que representa un jugador en el ranking")
public class PlayerRankingDTO {
    @Schema(description = "ID del jugador", example = "5")
    private String playerId;
    @Schema(description = "Posición en el ranking", example = "1")
    private Integer rank;
    @Schema(description = "Puntuación del jugador", example = "95.5")
    private BigDecimal score;
    @Schema(description = "Nombre del jugador", example = "Cristiano Ronaldo")
    private String playerName;
    @Schema(description = "Equipo del jugador", example = "Al Nassr")
    private String team;
    @Schema(description = "Posición del jugador", example = "Forward")
    private String position;
    @Schema(description = "Liga del jugador", example = "LALIGA")
    private String league;
    @Schema(description = "Posición alternativa del jugador", example = "Midfielder")
    private String altPosition;

    public static PlayerRankingDTO of(Player p, int rank) {
        return PlayerRankingDTO.builder()
                .playerId(p.getId() == null ? null : p.getId().toString())
                .rank(rank)
                .score(p.getScore())
                .playerName(p.getName())
                .team(p.getTeam())
                .position(p.getPosition())
                .league(p.getLeague())
                .altPosition(p.getAltPosition())
                .build();
    }
}

