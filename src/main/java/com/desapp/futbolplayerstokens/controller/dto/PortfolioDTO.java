package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Portfolio;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PortfolioDTO {
    private Long playerId;
    private int tokenQty;
    private BigDecimal avgBuyPrice;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;

    public static PortfolioDTO toDTO(Portfolio p) {
        if (p == null) return null;
        return PortfolioDTO.builder()
                .playerId(p.getPlayerId())
                .tokenQty(p.getTokenQty())
                .avgBuyPrice(p.getAvgBuyPrice())
                .currentValue(p.getCurrentValue())
                .profitLoss(p.getProfitLoss())
                .build();
    }
}

