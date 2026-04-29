package com.desapp.futbolplayerstokens.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "football-data.api")
public class FootballDataProperties {
    private String baseUrl;
    private String token;
    private List<String> competitionIds;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<String> getCompetitionIds() {
        return competitionIds;
    }

    public void setCompetitionIds(List<String> competitionIds) {
        this.competitionIds = competitionIds;
    }
}
