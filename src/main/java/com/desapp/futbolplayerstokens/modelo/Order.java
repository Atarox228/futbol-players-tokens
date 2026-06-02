package com.desapp.futbolplayerstokens.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long playerId;

    @Enumerated(EnumType.STRING)
    private OrderType type;

    public enum OrderType { BUY, SELL }

    private int quantity;

    @Column(precision = 19, scale = 8)
    private BigDecimal priceAtOrder;

    @Column(precision = 19, scale = 8)
    private BigDecimal total;

    @Column(unique = true)
    private String idempotencyKey;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}

