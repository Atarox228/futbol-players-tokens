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
    private String type;
    private int quantity;
    private BigDecimal priceAtOrder;
    private BigDecimal total;
    private String idempotencyKey;
    private LocalDateTime createdAt;

    public static OrderDTO toDTO(Order o) {
        if (o == null) return null;
        return OrderDTO.builder()
                .id(o.getId())
                .userId(o.getUserId())
                .playerId(o.getPlayerId())
                .type(o.getType() == null ? null : o.getType().name())
                .quantity(o.getQuantity())
                .priceAtOrder(o.getPriceAtOrder())
                .total(o.getTotal())
                .idempotencyKey(o.getIdempotencyKey())
                .createdAt(o.getCreatedAt())
                .build();
    }
}

