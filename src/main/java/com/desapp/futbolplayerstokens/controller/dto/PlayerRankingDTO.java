package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Player;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlayerRankingDTO {
    private String playerId;
    private Integer rank;
    private BigDecimal score;

    public static PlayerRankingDTO of(Player p, int rank) {
        return PlayerRankingDTO.builder()
                .playerId(p.getId() == null ? null : p.getId().toString())
                .rank(rank)
                .score(p.getScore())
                .build();
    }
}

