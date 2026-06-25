package com.desapp.futbolplayerstokens.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerValuationDTO {
    private Long playerId;
    private String playerName;
    private BigDecimal currentPrice;
    private BigDecimal priceChange1d;
    private BigDecimal priceChange7d;
    private BigDecimal priceChange30d;
    private double volatility30d;
    private BigDecimal score;
    private String position;
    private String team;
}
