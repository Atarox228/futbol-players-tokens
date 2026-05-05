package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Player;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlayerDTO {
    private Long id;
    private String name;
    private String team;
    private String league;
    private String position;
    private BigDecimal score;

    public static PlayerDTO toDTO(Player player) {
        return PlayerDTO.builder()
                .id(player.getId())
                .name(player.getName())
                .team(player.getTeam())
                .league(player.getLeague())
                .position(player.getPosition())
                .score(player.getScore())
                .build();
    }
}
