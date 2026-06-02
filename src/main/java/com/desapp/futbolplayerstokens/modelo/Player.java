package com.desapp.futbolplayerstokens.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    private static final int DEFAULT_COUNT = 1;
    private static final double DEFAULT_PASS_ACCURACY = 1.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String team;
    private String league;
    private String position;
    private Double rating;
    private Integer appearances;
    private Integer minutes;
    private Integer goals;
    private Integer assists;
    @Builder.Default
    private Double shotsOnTarget = 0.0;
    
    @Builder.Default
    private Double aerialWon = 0.0;
    @Builder.Default
    private Double faults = 0.0;
    @Builder.Default
    private Double offsidesGiven = 0.0;
    @Builder.Default
    private Double clears = 0.0;
    @Builder.Default
    private Double dribbled = 0.0;
    @Builder.Default
    private Double tackles = 0.0;
    @Builder.Default
    private Double interceptions = 0.0;
    @Builder.Default
    private Double blocks = 0.0;
    @Builder.Default
    private Integer ownGoals = 1;
    @Builder.Default
    private Double keyPasses = 0.0;
    @Builder.Default
    private Double dribbles = 0.0;
    @Builder.Default
    private Double faulted = 0.0;
    @Builder.Default
    private Double offsides = 0.0;
    @Builder.Default
    private Double dispossesed = 0.0;
    @Builder.Default
    private Double turnover = 0.0;
    @Builder.Default
    private Double passAccuracy = 1.0;
    @Column(nullable = false)
    private int availableTokens;

    @Column(nullable = false)
    private int totalTokens;
    private Integer yellowCards;
    private Integer redCards;
    private Integer playerOfTheMatch;
    @Column(precision = 19, scale = 8)
    private BigDecimal score;
    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;


    @PrePersist
    @PreUpdate
    public void applyPersistenceDefaults() {
        shotsOnTarget = defaultIfNull(shotsOnTarget, 0.0);
        
        aerialWon = defaultIfNull(aerialWon, 0.0);
        faults = defaultIfNull(faults, 0.0);
        offsidesGiven = defaultIfNull(offsidesGiven, 0.0);
        clears = defaultIfNull(clears, 0.0);
        dribbled = defaultIfNull(dribbled, 0.0);
        tackles = defaultIfNull(tackles, 0.0);
        interceptions = defaultIfNull(interceptions, 0.0);
        blocks = defaultIfNull(blocks, 0.0);
        ownGoals = defaultIfNull(ownGoals, DEFAULT_COUNT);
        keyPasses = defaultIfNull(keyPasses, 0.0);
        dribbles = defaultIfNull(dribbles, 0.0);
        faulted = defaultIfNull(faulted, 0.0);
        offsides = defaultIfNull(offsides, 0.0);
        dispossesed = defaultIfNull(dispossesed, 0.0);
        turnover = defaultIfNull(turnover, 0.0);
        passAccuracy = (passAccuracy == null || passAccuracy.doubleValue() == 0.0) ? DEFAULT_PASS_ACCURACY : passAccuracy;
        if (availableTokens == 0 && totalTokens == 0) {
            availableTokens = 100;
            totalTokens = 100;
        }
        lastModifiedAt = LocalDateTime.now();
    }

    private Double defaultIfNull(Double value, Double defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }
}
