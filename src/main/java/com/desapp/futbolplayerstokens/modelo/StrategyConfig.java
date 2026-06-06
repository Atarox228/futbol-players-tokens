package com.desapp.futbolplayerstokens.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "strategy_configs", uniqueConstraints = @UniqueConstraint(columnNames = {"type", "version"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategyConfig {

    public enum StrategyType {
        GENERAL, FORWARD, MIDFIELDER, DEFENDER, GOALKEEPER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal valorBase;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal factorEscala;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StrategyType type;

    @Column(nullable = false)
    private Integer version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "strategy_config_weights", joinColumns = @JoinColumn(name = "strategy_config_id"))
    @MapKeyColumn(name = "metric_name")
    @Column(name = "metric_weight", precision = 19, scale = 8)
    @Builder.Default
    private Map<String, BigDecimal> weights = new HashMap<>();
}

