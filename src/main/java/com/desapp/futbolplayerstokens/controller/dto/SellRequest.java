package com.desapp.futbolplayerstokens.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud de venta de jugador")
public class SellRequest {
    @Schema(description = "ID del jugador", example = "1")
    private Long playerId;
    @Schema(description = "Cantidad de tokens a vender", example = "5")
    private int quantity;
    @Schema(description = "Clave de idempotencia", example = "unique-key-456")
    private String idempotencyKey;
    @Schema(description = "Precio mínimo por token", example = "100.00")
    private BigDecimal minPrice;
}
