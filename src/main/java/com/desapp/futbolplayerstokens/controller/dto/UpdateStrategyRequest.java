package com.desapp.futbolplayerstokens.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStrategyRequest {
    private BigDecimal valorBase;
    private BigDecimal factorEscala;
    private Map<String, BigDecimal> weights;
}
