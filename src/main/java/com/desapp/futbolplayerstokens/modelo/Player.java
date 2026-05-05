package com.desapp.futbolplayerstokens.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

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

    @PrePersist
    @PreUpdate
    public void applyDefaults() {
        if (played == null) {
            played = 1;
        }
        if (won == null) {
            won = 1;
        }
        if (lost == null) {
            lost = 1;
        }
        if (shotsOnTarget == null) {
            shotsOnTarget = 1;
        }
        if (clears == null) {
            clears = 1;
        }
        if (goalsConceded == null) {
            goalsConceded = 1;
        }
        if (tackles == null) {
            tackles = 1;
        }
        if (interceptions == null) {
            interceptions = 1;
        }
        if (blocks == null) {
            blocks = 1;
        }
        if (ownGoals == null) {
            ownGoals = 1;
        }
        if (keyPasses == null) {
            keyPasses = 1;
        }
        if (passAccuracy == null) {
            passAccuracy = 1.0;
        }
    }
}