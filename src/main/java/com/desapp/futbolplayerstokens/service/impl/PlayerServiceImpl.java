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
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    public Page<PlayerDTO> getPlayersWithFilters(String league, String team, String position, Pageable pageable) {
        return playerRepository.findByFilters(league, team, position, pageable)
                .map(PlayerDTO::toDTO);
    }

    @Override
    public void saveAllPlayers(List<PlayerDetailDTO> playerDTOs) {
        int saved = 0;
        for (PlayerDetailDTO dto : playerDTOs) {
            try {
                if (!playerRepository.findByNameIgnoreCaseAndTeamIgnoreCase(
                        dto.getName().trim(), dto.getTeam().trim()).isEmpty()) {
                    continue;
                }

                saveNewPlayer(dto);
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
                saveNewPlayer(dto);
                insertedRows++;
                continue;
            }

            updateExistingPlayers(dto, matches);
            modifiedRows += matches.size();
        }

        int rosterFound = playerDTOs.size();
        return new PlayerOverwriteResult(rosterFound, modifiedRows, insertedRows);
    }

    private void saveNewPlayer(PlayerDetailDTO dto) {
        Player player = Player.builder()
            .name(dto.getName())
            .rating(dto.getRating())
            .team(dto.getTeam())
            .league(dto.getLeague())
            .position(dto.getPosition())
            .altPosition(dto.getAltPosition())
            .appearances(dto.getAppearances())
            .minutes(dto.getMinutes())
            .goals(dto.getGoals())
            .assists(dto.getAssists())
            .shotsOnTarget(dto.getShotsOnTarget())
            .passAccuracy(dto.getPassAccuracy())
            .aerialWon(dto.getAerialWon())
            .faults(dto.getFaults())
            .offsidesGiven(dto.getOffsidesGiven())
            .clears(dto.getClears())
            .dribbled(dto.getDribbled())
            .tackles(dto.getTackles())
            .interceptions(dto.getInterceptions())
            .blocks(dto.getBlocks())
            .ownGoals(dto.getOwnGoals())
            .keyPasses(dto.getKeyPasses())
            .dribbles(dto.getDribbles())
            .faulted(dto.getFaulted())
            .offsides(dto.getOffsides())
            .dispossesed(dto.getDispossesed())
            .turnover(dto.getTurnover())
            .passAccuracy(dto.getPassAccuracy())
            .yellowCards(dto.getYellowCards())
            .redCards(dto.getRedCards())
            .playerOfTheMatch(dto.getPlayerOfTheMatch())
            .build();
        playerRepository.save(player);
    }

    private void updateExistingPlayers(PlayerDetailDTO dto, List<Player> players) {
        for (Player player : players) {
            player.setRating(dto.getRating());
            player.setAppearances(dto.getAppearances());
            player.setMinutes(dto.getMinutes());
            player.setGoals(dto.getGoals());
            player.setAssists(dto.getAssists());
            player.setShotsOnTarget(dto.getShotsOnTarget());
            player.setPassAccuracy(dto.getPassAccuracy());
            player.setAerialWon(dto.getAerialWon());
            player.setFaults(dto.getFaults());
            player.setOffsidesGiven(dto.getOffsidesGiven());
            player.setClears(dto.getClears());
            player.setDribbled(dto.getDribbled());
            player.setTackles(dto.getTackles());
            player.setInterceptions(dto.getInterceptions());
            player.setBlocks(dto.getBlocks());
            player.setOwnGoals(dto.getOwnGoals());
            player.setKeyPasses(dto.getKeyPasses());
            player.setDribbles(dto.getDribbles());
            player.setFaulted(dto.getFaulted());
            player.setOffsides(dto.getOffsides());
            player.setDispossesed(dto.getDispossesed());
            player.setTurnover(dto.getTurnover());
            player.setPassAccuracy(dto.getPassAccuracy());
            player.setYellowCards(dto.getYellowCards());
            player.setRedCards(dto.getRedCards());
            player.setPlayerOfTheMatch(dto.getPlayerOfTheMatch());
            if (dto.getPosition() != null && !dto.getPosition().isBlank()) {
                player.setPosition(dto.getPosition());
            }
            if (dto.getAltPosition() != null && !dto.getAltPosition().isBlank()) {
                player.setAltPosition(dto.getAltPosition());
            }
            player.setLastModifiedAt(LocalDateTime.now());
        }
        playerRepository.saveAll(players);
    }
}

