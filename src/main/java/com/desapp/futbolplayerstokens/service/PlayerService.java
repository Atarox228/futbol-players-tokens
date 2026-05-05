package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;
import com.desapp.futbolplayerstokens.controller.dto.PlayerDetailDTO;

import java.util.List;

public interface PlayerService {
    PlayerDetailDTO getPlayerById(Long id);
    List<PlayerDTO> getPlayersWithFilters(String league, String team, String position);
    void saveAllPlayers(List<PlayerDetailDTO> playerDTOs);
    PlayerOverwriteResult overwritePlayersByNameAndTeam(List<PlayerDetailDTO> playerDTOs);
    boolean playerExists(String name, String team);
    List<PlayerDTO> getPlayersByTeam(String team);
}
