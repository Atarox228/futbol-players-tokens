package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PlayerDTO;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.service.PlayerService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerServiceImpl(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public PlayerDTO getPlayerById(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        return PlayerDTO.toDTO(player);
    }

    @Override
    public void saveAllPlayers(List<PlayerDTO> playerDTOs) {
        int saved = 0;
        for (PlayerDTO dto : playerDTOs) {
            try {
                // Verificar si el jugador ya existe en la BD (por nombre + liga + equipo)
                if (playerRepository.findAll().stream()
                        .anyMatch(p -> p.getName().equals(dto.getName()) &&
                                      p.getLeague().equals(dto.getLeague()) &&
                                      p.getTeam().equals(dto.getTeam()))) {
                    System.out.println("⚠️  Jugador " + dto.getName() + " ya existe, saltando...");
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
        System.out.println("\n✓ Se guardaron " + saved + " jugadores en la BD");
    }
}

