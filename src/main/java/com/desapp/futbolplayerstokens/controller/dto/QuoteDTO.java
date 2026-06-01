package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Quote;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(name = "QuoteDTO", description = "DTO que representa una cotización de precio de un jugador")
public class QuoteDTO {
    @Schema(description = "ID único de la cotización", example = "1")
    private Long id;
    @Schema(description = "ID del jugador", example = "5")
    private Long playerId;
    @Schema(description = "Precio de cotización", example = "1250.50")
    private BigDecimal price;
    @Schema(description = "Fecha y hora de la cotización", example = "2026-06-01T10:30:00")
    private LocalDateTime timestamp;
    @Schema(description = "ID de la estrategia", example = "1")
    private Long strategyId;
    @Schema(description = "Versión de la estrategia", example = "1")
    private Integer strategyVersion;
    @Schema(description = "Disparador de la cotización", example = "MANUAL")
    private String trigger;

    public static QuoteDTO toDTO(Quote q) {
        return QuoteDTO.builder()
                .id(q.getId())
                .playerId(q.getPlayer() != null ? q.getPlayer().getId() : null)
                .price(q.getPrice())
                .timestamp(q.getTimestamp())
                .strategyId(q.getStrategyId())
                .strategyVersion(q.getStrategyVersion())
                .trigger(q.getTrigger() == null ? null : q.getTrigger().name())
                .build();
    }
}


