package com.desapp.futbolplayerstokens.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValuationResult {
    private BigDecimal price;
    private Long strategyId;
    private Integer strategyVersion;
}

