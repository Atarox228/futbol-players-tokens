package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;
import java.util.List;

public interface PlayerService {
    PlayerDTO getPlayerById(Long id);
    void saveAllPlayers(List<PlayerDTO> playerDTOs);
    PlayerOverwriteResult overwritePlayersByNameAndTeam(List<PlayerDTO> playerDTOs);
    boolean playerExists(String name, String team);
}

