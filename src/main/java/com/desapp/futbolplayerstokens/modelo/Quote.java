package com.desapp.futbolplayerstokens.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private Long strategyId;

    private Integer strategyVersion;

    @Enumerated(EnumType.STRING)
    private QuoteTrigger trigger;

    @Column(nullable = false)
    private LocalDateTime lastModifiedAt;

    @PrePersist
    public void prePersist() {
        this.lastModifiedAt = LocalDateTime.now();
        if (this.timestamp == null) this.timestamp = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.lastModifiedAt = LocalDateTime.now();
    }
}


