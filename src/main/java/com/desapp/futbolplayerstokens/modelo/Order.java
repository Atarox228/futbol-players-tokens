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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    @Column(nullable = false)
    private int quantity;

    @Column(precision = 19, scale = 8, nullable = false)
    private BigDecimal priceAtOrder;

    @Column(precision = 19, scale = 8, nullable = false)
    private BigDecimal total;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private int remainingQuantity;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = OrderStatus.PENDING;
        }
        if (remainingQuantity == 0) {
            remainingQuantity = quantity;
        }
    }

    public enum OrderType { BUY, SELL }
    public enum OrderStatus { PENDING, PARTIALLY_FILLED, FILLED, CANCELLED }
}
