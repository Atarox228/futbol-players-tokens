package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderDTO {
    private Long id;
    private Long userId;
    private Long playerId;
    private String playerName;
    private String type;
    private int quantity;
    private BigDecimal priceAtOrder;
    private BigDecimal total;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private String status;
    private int remainingQuantity;

    public static OrderDTO toDTO(Order o) {
        if (o == null) return null;
        return OrderDTO.builder()
                .id(o.getId())
                .userId(o.getUser().getId())
                .playerId(o.getPlayer().getId())
                .playerName(o.getPlayer().getName())
                .type(o.getType().name())
                .quantity(o.getQuantity())
                .priceAtOrder(o.getPriceAtOrder())
                .total(o.getTotal())
                .idempotencyKey(o.getIdempotencyKey())
                .createdAt(o.getCreatedAt())
                .status(o.getStatus().name())
                .remainingQuantity(o.getRemainingQuantity())
                .build();
    }
}

