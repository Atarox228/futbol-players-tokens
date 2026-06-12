package com.desapp.futbolplayerstokens.modelo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "football_data_match_id")
    private Long footballDataMatchId;

    private Long team1Id;
    private Long team2Id;
    private LocalDateTime matchTime;
    private String status;
}
