package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;
import com.desapp.futbolplayerstokens.controller.dto.PlayerDetailDTO;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlayerService {
    PlayerDetailDTO getPlayerById(Long id);
    Page<PlayerDTO> getPlayersWithFilters(String league, String team, String position, Pageable pageable);
    void saveAllPlayers(List<PlayerDetailDTO> playerDTOs);
    PlayerOverwriteResult overwritePlayersByNameAndTeam(List<PlayerDetailDTO> playerDTOs);
    boolean playerExists(String name, String team);
}
