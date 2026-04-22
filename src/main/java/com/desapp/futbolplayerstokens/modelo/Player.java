package com.desapp.futbolplayerstokens.modelo;

import jakarta.persistence.*;
import lombok.*;

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
    private Double rating;
    private String team;
    private String league;
    private String position;
    private Integer appearances;
    private Integer minutes;
    private Integer goals;
    private Integer assists;
    private Integer yellowCards;
    private Integer redCards;
    private Integer playerOfTheMatch;
}