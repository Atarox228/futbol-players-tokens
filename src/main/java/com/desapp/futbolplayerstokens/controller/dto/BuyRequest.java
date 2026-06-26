package com.desapp.futbolplayerstokens.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud de compra de jugador")
public class BuyRequest {
    @Schema(description = "ID del jugador", example = "1")
    private Long playerId;
    @Schema(description = "Cantidad de tokens a comprar", example = "10")
    private int quantity;
    @Schema(description = "Clave de idempotencia", example = "unique-key-123")
    private String idempotencyKey;
    @Schema(description = "Precio máximo por token", example = "150.00")
    private BigDecimal maxPrice;
}
