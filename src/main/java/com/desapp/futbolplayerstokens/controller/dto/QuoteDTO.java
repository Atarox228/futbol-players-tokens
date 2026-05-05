package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Quote;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class QuoteDTO {
    private Long id;
    private Long playerId;
    private BigDecimal price;
    private LocalDateTime timestamp;
    private Long strategyId;
    private Integer strategyVersion;
    private String trigger;

    public static QuoteDTO toDTO(Quote q) {
        return QuoteDTO.builder()
                .id(q.getId())
                .playerId(q.getPlayer() != null ? q.getPlayer().getId() : null)
                .price(q.getPrice())
                .timestamp(q.getTimestamp())
                .strategyId(q.getStrategyId())
                .strategyVersion(q.getStrategyVersion())
                .trigger(q.getTrigger() == null ? null : q.getTrigger().name())
                .build();
    }
}


