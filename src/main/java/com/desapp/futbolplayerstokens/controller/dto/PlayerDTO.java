package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Player;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerDTO {
    private Long id;
    private String name;
    private Double rating;
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


    public static PlayerDTO toDTO(Player player) {
        return PlayerDTO.builder()
                .id(player.getId())
                .name(player.getName())
                .rating(player.getRating())
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
                .build();
    }
}
