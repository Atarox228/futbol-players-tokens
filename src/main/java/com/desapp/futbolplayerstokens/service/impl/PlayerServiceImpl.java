package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;
import com.desapp.futbolplayerstokens.controller.dto.PlayerDetailDTO;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.service.PlayerOverwriteResult;
import com.desapp.futbolplayerstokens.service.PlayerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerServiceImpl(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public PlayerDetailDTO getPlayerById(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        return PlayerDetailDTO.toDTO(player);
    }

    @Override
    public List<PlayerDTO> getPlayersWithFilters(String league, String team, String position) {
        List<Player> players = playerRepository.findByFilters(league, team, position);
        return players.stream()
                .map(PlayerDTO::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void saveAllPlayers(List<PlayerDetailDTO> playerDTOs) {
        int saved = 0;
        for (PlayerDetailDTO dto : playerDTOs) {
            try {
                // Verificar si el jugador ya existe en la BD (por nombre + liga + equipo)
                if (playerRepository.findAll().stream()
                        .anyMatch(p -> p.getName().equals(dto.getName()) &&
                                      p.getLeague().equals(dto.getLeague()) &&
                                      p.getTeam().equals(dto.getTeam()))) {
                    continue;
                }

                Player player = Player.builder()
                        .name(dto.getName())
                        .rating(dto.getRating())
                        .team(dto.getTeam())
                        .league(dto.getLeague())
                        .position(dto.getPosition())
                        .appearances(dto.getAppearances())
                        .minutes(dto.getMinutes())
                        .goals(dto.getGoals())
                        .assists(dto.getAssists())
                        .yellowCards(dto.getYellowCards())
                        .redCards(dto.getRedCards())
                        .playerOfTheMatch(dto.getPlayerOfTheMatch())
                        .build();

                playerRepository.save(player);
                saved++;
            } catch (Exception e) {
                System.err.println("Error guardando jugador " + dto.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public boolean playerExists(String name, String team) {
        return playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(name.trim(), team.trim())
                .stream()
                .findAny()
                .isPresent();
    }

    @Override
    @Transactional
    public PlayerOverwriteResult overwritePlayersByNameAndTeam(List<PlayerDetailDTO> playerDTOs) {
        int modifiedRows = 0;
        int insertedRows = 0;

        for (PlayerDetailDTO dto : playerDTOs) {
            if (dto.getName() == null || dto.getName().isBlank() || dto.getTeam() == null || dto.getTeam().isBlank()) {
                continue;
            }

            List<Player> matches = playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(
                dto.getName().trim(),
                dto.getTeam().trim()
            );

            if (matches.isEmpty()) {
                Player player = Player.builder()
                    .name(dto.getName())
                    .rating(dto.getRating())
                    .team(dto.getTeam())
                    .league(dto.getLeague())
                    .position(dto.getPosition())
                    .appearances(dto.getAppearances())
                    .minutes(dto.getMinutes())
                    .goals(dto.getGoals())
                    .assists(dto.getAssists())
                    .yellowCards(dto.getYellowCards())
                    .redCards(dto.getRedCards())
                    .playerOfTheMatch(dto.getPlayerOfTheMatch())
                    .build();
                playerRepository.save(player);
                insertedRows++;
                continue;
            }

            for (Player player : matches) {
                // Solo actualizar campos de estadísticas, NO sobreescribir: name, team, league, position
                player.setRating(dto.getRating());
                player.setAppearances(dto.getAppearances());
                player.setMinutes(dto.getMinutes());
                player.setGoals(dto.getGoals());
                player.setAssists(dto.getAssists());
                player.setYellowCards(dto.getYellowCards());
                player.setRedCards(dto.getRedCards());
                player.setPlayerOfTheMatch(dto.getPlayerOfTheMatch());
                // Actualizar explícitamente el timestamp
                player.setLastModifiedAt(LocalDateTime.now());
            }

            playerRepository.saveAll(matches);
            modifiedRows += matches.size();
        }

        int rosterFound = playerDTOs.size();
        return new PlayerOverwriteResult(rosterFound, modifiedRows, insertedRows);
    }
}

