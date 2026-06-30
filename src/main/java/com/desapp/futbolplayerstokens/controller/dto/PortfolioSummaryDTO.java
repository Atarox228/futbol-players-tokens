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
public class PortfolioSummaryDTO {
    private Long userId;
    private String username;
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPercent;
    private int totalPositions;
    private List<PositionSummary> positions;
    private Diversification diversification;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PositionSummary {
        private Long playerId;
        private String playerName;
        private String position;
        private String team;
        private int tokenQty;
        private BigDecimal avgBuyPrice;
        private BigDecimal currentPrice;
        private BigDecimal currentValue;
        private BigDecimal profitLoss;
        private BigDecimal profitLossPercent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Diversification {
        private long forwardCount;
        private long midfielderCount;
        private long defenderCount;
        private long goalkeeperCount;
        private long laLigaCount;
        private long premierLeagueCount;
        private long bundesligaCount;
        private long serieACount;
        private long ligue1Count;
    }
}
