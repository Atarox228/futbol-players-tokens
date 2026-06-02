package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Portfolio;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Builder
public class PortfolioDTO {
    private Long playerId;
    private String playerName;
    private int tokenQty;
    private BigDecimal avgBuyPrice;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
    private BigDecimal currentPrice;

    public static PortfolioDTO of(Portfolio p, BigDecimal currentPrice) {
        BigDecimal qty = BigDecimal.valueOf(p.getTokenQty());
        BigDecimal currentValue = currentPrice.multiply(qty).setScale(8, RoundingMode.HALF_UP);
        BigDecimal profitLoss = currentPrice.subtract(p.getAvgBuyPrice()).multiply(qty).setScale(8, RoundingMode.HALF_UP);
        return PortfolioDTO.builder()
                .playerId(p.getPlayer().getId())
                .playerName(p.getPlayer().getName())
                .tokenQty(p.getTokenQty())
                .avgBuyPrice(p.getAvgBuyPrice())
                .currentValue(currentValue)
                .profitLoss(profitLoss)
                .currentPrice(currentPrice)
                .build();
    }
}
