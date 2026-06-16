package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Player;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(name = "PlayerDTO", description = "DTO que representa un jugador de fútbol")
public class PlayerDTO {
    @Schema(description = "ID único del jugador", example = "1")
    private Long id;
    @Schema(description = "Nombre del jugador", example = "Cristiano Ronaldo")
    private String name;
    @Schema(description = "Equipo del jugador", example = "Manchester United")
    private String team;
    @Schema(description = "Liga del jugador", example = "LALIGA")
    private String league;
    @Schema(description = "Posición del jugador", example = "Forward")
    private String position;
    @Schema(description = "Puntuación del jugador", example = "85.5")
    private BigDecimal score;
    @Schema(description = "Posición alternativa del jugador", example = "Midfielder")
    private String altPosition;
    @Schema(description = "Tokens disponibles del jugador", example = "100")
    private int availableTokens;
    @Schema(description = "Total de tokens del jugador", example = "100")
    private int totalTokens;

    public static PlayerDTO toDTO(Player player) {
        return PlayerDTO.builder()
                .id(player.getId())
                .name(player.getName())
                .team(player.getTeam())
                .league(player.getLeague())
                .position(player.getPosition())
                .score(player.getScore())
                .altPosition(player.getAltPosition())
                .availableTokens(player.getAvailableTokens())
                .totalTokens(player.getTotalTokens())
                .build();
    }
    
}
