package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Player;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlayerDetailDTO {
    private Long id;
    private String name;
    private Double rating;
    private Integer played;
    private Integer won;
    private Integer lost;
    private String team;
    private String league;
    private String position;
    private Integer appearances;
    private Integer minutes;
    private Integer goals;
    private Integer assists;
    private Integer yellowCards;
    private Integer redCards;
    private Integer playerOfTheMatch;
    private BigDecimal score;

    public static PlayerDetailDTO toDTO(Player player) {
        return PlayerDetailDTO.builder()
                .id(player.getId())
                .name(player.getName())
                .rating(player.getRating())
                .played(player.getPlayed())
                .won(player.getWon())
                .lost(player.getLost())
                .team(player.getTeam())
                .league(player.getLeague())
                .position(player.getPosition())
                .appearances(player.getAppearances())
                .minutes(player.getMinutes())
                .goals(player.getGoals())
                .assists(player.getAssists())
                .yellowCards(player.getYellowCards())
                .redCards(player.getRedCards())
                .playerOfTheMatch(player.getPlayerOfTheMatch())
                .score(player.getScore())
                .build();
    }
}

