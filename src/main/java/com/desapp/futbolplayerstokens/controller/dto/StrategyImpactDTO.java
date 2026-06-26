package com.desapp.futbolplayerstokens.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategyImpactDTO {
    private String strategyType;
    private int previousVersion;
    private int currentVersion;
    private BigDecimal avgPriceBefore;
    private BigDecimal avgPriceAfter;
    private BigDecimal priceChangePercent;
    private long affectedPlayers;
    private List<PriceChange> topChanges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceChange {
        private Long playerId;
        private String playerName;
        private BigDecimal oldPrice;
        private BigDecimal newPrice;
        private BigDecimal changePercent;
    }
}
