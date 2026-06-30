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
public class TopTradedDTO {
    private int rank;
    private Long playerId;
    private String playerName;
    private String team;
    private String league;
    private long orderCount;
    private long totalQuantity;
    private BigDecimal totalValue;
}
