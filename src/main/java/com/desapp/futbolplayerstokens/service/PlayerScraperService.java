package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDetailDTO;

import java.util.List;
import java.util.function.Consumer;

public interface PlayerScraperService {
    List<PlayerDetailDTO> scrapeAllPlayers(String url, String league, Consumer<List<PlayerDetailDTO>> onPageComplete);

    List<PlayerDetailDTO> scrapeAllPlayers(String url, String league, Consumer<List<PlayerDetailDTO>> onPageComplete, boolean clearTable);

    List<PlayerDetailDTO> scrapeTeamPlayersByName(String teamName, String league);

    List<PlayerDetailDTO> scrapeNewPlayersOnly(String url, String league, Consumer<List<PlayerDetailDTO>> onPageComplete);

    void scrapeAllPlayersIfDatabaseEmpty();
}
