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

    public static PlayerRankingDTO of(Player p, int rank) {
        return PlayerRankingDTO.builder()
                .playerId(p.getId() == null ? null : p.getId().toString())
                .rank(rank)
                .score(p.getScore())
                .build();
    }
}

