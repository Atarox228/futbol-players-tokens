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
public class MarketDepthDTO {
    private Long playerId;
    private String playerName;
    private BigDecimal bestBid;
    private BigDecimal bestAsk;
    private BigDecimal spread;
    private List<DepthLevel> bids;
    private List<DepthLevel> asks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepthLevel {
        private BigDecimal price;
        private int totalQuantity;
        private int orderCount;
    }
}
