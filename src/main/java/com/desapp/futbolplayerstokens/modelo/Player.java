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
    }
}