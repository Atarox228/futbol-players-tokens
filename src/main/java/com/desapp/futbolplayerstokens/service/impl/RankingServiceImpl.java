package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PlayerRankingDTO;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.service.RankingService;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RankingServiceImpl implements RankingService {

    private final PlayerRepository playerRepository;

    public RankingServiceImpl(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    @Cacheable(value = "playerRanking", key = "{#page, #size}")
    public List<PlayerRankingDTO> getRanking(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;

        Pageable pageable = PageRequest.of(page, size);
        List<Player> players = playerRepository.findRankedPlayers(pageable);

        List<PlayerRankingDTO> dtos = new ArrayList<>();
        int rankStart = page * size + 1;
        for (int i = 0; i < players.size(); i++) {
            dtos.add(PlayerRankingDTO.of(players.get(i), rankStart + i));
        }
        return dtos;
    }
}
