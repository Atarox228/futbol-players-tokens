package com.desapp.futbolplayerstokens.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchApiDTO {

    @JsonProperty("matches")
    private List<Match> matches;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Match {
        private Long id;

        @JsonProperty("utcDate")
        private OffsetDateTime matchTime;

        private String status;

        @JsonProperty("homeTeam")
        private Team homeTeam;

        @JsonProperty("awayTeam")
        private Team awayTeam;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private Long id;
        private String name;
    }
}
