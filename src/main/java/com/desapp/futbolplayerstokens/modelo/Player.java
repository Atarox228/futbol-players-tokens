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
    @Builder.Default
    private Integer played = 1;
    @Builder.Default
    private Integer won = 1;
    @Builder.Default
    private Integer lost = 1;
    private Integer appearances;
    private Integer minutes;
    private Integer goals;
    private Integer assists;
    @Builder.Default
    private Integer shotsOnTarget = 1;
    @Builder.Default
    private Integer clears = 1;
    @Builder.Default
    private Integer goalsConceded = 1;
    @Builder.Default
    private Integer tackles = 1;
    @Builder.Default
    private Integer interceptions = 1;
    @Builder.Default
    private Integer blocks = 1;
    @Builder.Default
    private Integer ownGoals = 1;
    @Builder.Default
    private Integer keyPasses = 1;
    @Builder.Default
    private Double passAccuracy = 1.0;
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
        played = defaultIfNull(played, DEFAULT_COUNT);
        won = defaultIfNull(won, DEFAULT_COUNT);
        lost = defaultIfNull(lost, DEFAULT_COUNT);
        shotsOnTarget = defaultIfNull(shotsOnTarget, DEFAULT_COUNT);
        clears = defaultIfNull(clears, DEFAULT_COUNT);
        goalsConceded = defaultIfNull(goalsConceded, DEFAULT_COUNT);
        tackles = defaultIfNull(tackles, DEFAULT_COUNT);
        interceptions = defaultIfNull(interceptions, DEFAULT_COUNT);
        blocks = defaultIfNull(blocks, DEFAULT_COUNT);
        ownGoals = defaultIfNull(ownGoals, DEFAULT_COUNT);
        keyPasses = defaultIfNull(keyPasses, DEFAULT_COUNT);
        passAccuracy = defaultIfNull(passAccuracy, DEFAULT_PASS_ACCURACY);
        lastModifiedAt = LocalDateTime.now();
    }

    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Double defaultIfNull(Double value, Double defaultValue) {
        return value == null ? defaultValue : value;
    }
}