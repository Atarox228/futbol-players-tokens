package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;
import java.util.List;
import java.util.function.Consumer;

public interface PlayerScraperService {
    List<PlayerDTO> scrapeAllPlayers(String url, String league, Consumer<List<PlayerDTO>> onPageComplete);

    List<PlayerDTO> scrapeAllPlayers(String url, String league, Consumer<List<PlayerDTO>> onPageComplete, boolean clearTable);

    List<PlayerDTO> scrapeTeamPlayersByName(String teamName);
}
