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
    private String team;
    private String league;
    private String position;
    private Integer appearances;
    private Integer minutes;
    private Integer goals;
    private Integer assists;
    private Double shotsOnTarget;
    private Double aerialWon;
    private Double faults;
    private Double offsidesGiven;
    private Double clears;
    private Double dribbled;
    private Double tackles;
    private Double interceptions;
    private Double blocks;
    private Integer ownGoals;
    private Double keyPasses;
    private Double dribbles;
    private Double faulted;
    private Double offsides;
    private Double dispossesed;
    private Double turnover;
    private Double passAccuracy;
    private Integer yellowCards;
    private Integer redCards;
    private Integer playerOfTheMatch;
    private BigDecimal score;

    public static PlayerDetailDTO toDTO(Player player) {
        return PlayerDetailDTO.builder()
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
                .shotsOnTarget(player.getShotsOnTarget())
                .passAccuracy(player.getPassAccuracy())
            .aerialWon(player.getAerialWon())
            .faults(player.getFaults())
            .offsidesGiven(player.getOffsidesGiven())
                .clears(player.getClears())
            .dribbled(player.getDribbled())
                .tackles(player.getTackles())
                .interceptions(player.getInterceptions())
                .blocks(player.getBlocks())
                .ownGoals(player.getOwnGoals())
                .keyPasses(player.getKeyPasses())
            .dribbles(player.getDribbles())
            .faulted(player.getFaulted())
            .offsides(player.getOffsides())
            .dispossesed(player.getDispossesed())
            .turnover(player.getTurnover())
                .passAccuracy(player.getPassAccuracy())
                .yellowCards(player.getYellowCards())
                .redCards(player.getRedCards())
                .playerOfTheMatch(player.getPlayerOfTheMatch())
                .score(player.getScore())
                .build();
    }
}

