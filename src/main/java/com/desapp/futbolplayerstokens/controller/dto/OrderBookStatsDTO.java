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
public class OrderBookStatsDTO {
    private double fillRate;
    private double avgTimeToFillHours;
    private int avgOrderSize;
    private double cancelRate;
    private long totalOrders;
    private long filledOrders;
    private long cancelledOrders;
    private long pendingOrders;
}
