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
public class MarketOverviewDTO {
    private long openBuyOrders;
    private long openSellOrders;
    private BigDecimal totalValueLockedBuy;
    private BigDecimal totalValueLockedSell;
    private long activeUsers;
    private long totalPlayers;
    private long totalTokensInCirculation;
}
