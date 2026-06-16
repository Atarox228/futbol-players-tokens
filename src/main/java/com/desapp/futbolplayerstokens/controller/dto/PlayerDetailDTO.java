package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Player;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(name = "PlayerDetailDTO", description = "DTO detallado que representa un jugador de fútbol con sus estadísticas")
public class PlayerDetailDTO {
    @Schema(description = "ID único del jugador", example = "1")
    private Long id;
    @Schema(description = "Nombre del jugador", example = "Cristiano Ronaldo")
    private String name;
    @Schema(description = "Calificación del jugador", example = "8.5")
    private Double rating;
    @Schema(description = "Equipo del jugador", example = "Manchester United")
    private String team;
    @Schema(description = "Liga del jugador", example = "LALIGA")
    private String league;
    @Schema(description = "Posición del jugador", example = "Forward")
    private String position;
    @Schema(description = "Apariciones en partidos", example = "35")
    private Integer appearances;
    @Schema(description = "Minutos jugados", example = "2850")
    private Integer minutes;
    @Schema(description = "Goles anotados", example = "18")
    private Integer goals;
    @Schema(description = "Asistencias", example = "8")
    private Integer assists;
    @Schema(description = "Tiros a puerta", example = "45.5")
    private Double shotsOnTarget;
    @Schema(description = "Duelos aéreos ganados", example = "250")
    private Double aerialWon;
    @Schema(description = "Faltas cometidas", example = "25")
    private Double faults;
    @Schema(description = "Fueras de juego", example = "5")
    private Double offsidesGiven;
    @Schema(description = "Despejes", example = "120")
    private Double clears;
    @Schema(description = "Regates sufridos", example = "15")
    private Double dribbled;
    @Schema(description = "Entradas", example = "45")
    private Double tackles;
    @Schema(description = "Intercepciones", example = "60")
    private Double interceptions;
    @Schema(description = "Bloqueos", example = "30")
    private Double blocks;
    @Schema(description = "Goles en contra", example = "0")
    private Integer ownGoals;
    @Schema(description = "Pases clave", example = "25")
    private Double keyPasses;
    @Schema(description = "Regates realizados", example = "35")
    private Double dribbles;
    @Schema(description = "Faltas", example = "10")
    private Double faulted;
    @Schema(description = "Fueras de juego", example = "3")
    private Double offsides;
    @Schema(description = "Balones perdidos", example = "50")
    private Double dispossesed;
    @Schema(description = "Pérdidas de balón", example = "40")
    private Double turnover;
    @Schema(description = "Precisión de pases", example = "85.5")
    private Double passAccuracy;
    @Schema(description = "Tarjetas amarillas", example = "3")
    private Integer yellowCards;
    @Schema(description = "Tarjetas rojas", example = "0")
    private Integer redCards;
    @Schema(description = "Premios Mejor Jugador", example = "2")
    private Integer playerOfTheMatch;
    @Schema(description = "Puntuación del jugador", example = "85.5")
    private BigDecimal score;
    @Schema(description = "Posición alternativa del jugador", example = "Midfielder")
    private String altPosition;
    @Schema(description = "Tokens disponibles del jugador", example = "100")
    private int availableTokens;
    @Schema(description = "Total de tokens del jugador", example = "100")
    private int totalTokens;

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
                .altPosition(player.getAltPosition())
                .availableTokens(player.getAvailableTokens())
                .totalTokens(player.getTotalTokens())
                .build();
    }
}

