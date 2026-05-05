package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PlayerRankingDTO;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.service.RankingService;
import com.desapp.futbolplayerstokens.service.ActiveStrategyService;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class RankingServiceImpl implements RankingService {

    private final PlayerRepository playerRepository;
    private final ActiveStrategyService activeStrategyService;

    public RankingServiceImpl(PlayerRepository playerRepository, ActiveStrategyService activeStrategyService) {
        this.playerRepository = playerRepository;
        this.activeStrategyService = activeStrategyService;
    }

    @Override
    public List<PlayerRankingDTO> getRanking(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;

        long nonNullCount = playerRepository.countByScoreIsNotNull();
        long totalPlayers = playerRepository.count();

        long startIndex = (long) page * size; // 0-based global start
        long endIndexExclusive = Math.min(startIndex + size, totalPlayers);

        List<Player> resultPlayers = new ArrayList<>();

        if (startIndex < endIndexExclusive) {
            // fetch non-null players up to endIndexExclusive (we only need up to that)
            int fetchNonNullCount = (int) Math.min(nonNullCount, endIndexExclusive);
            if (fetchNonNullCount > 0) {
                List<Player> nonNullPlayers = playerRepository.findByScoreNotNullOrdered(PageRequest.of(0, fetchNonNullCount));
                // take the slice from startIndex to min(nonNullPlayers.size(), endIndexExclusive)
                int from = (int) Math.max(0, startIndex);
                int to = (int) Math.min(nonNullPlayers.size(), endIndexExclusive);
                if (from < to) {
                    resultPlayers.addAll(nonNullPlayers.subList(from, to));
                }
            }

            // if still need more (i.e., requested window spans into null-score players)
            int needed = (int) (endIndexExclusive - startIndex) - resultPlayers.size();
            if (needed > 0) {
                long nullOffset = Math.max(0, startIndex - nonNullCount);
                int fetchNullCount = (int) (nullOffset + needed);
                if (fetchNullCount > 0) {
                    List<Player> nullPlayers = playerRepository.findByScoreNullOrdered(PageRequest.of(0, fetchNullCount));
                    int fromNull = (int) nullOffset;
                    int toNull = Math.min(nullPlayers.size(), fromNull + needed);
                    if (fromNull < toNull) {
                        resultPlayers.addAll(nullPlayers.subList(fromNull, toNull));
                    }
                }
            }
        }

        List<PlayerRankingDTO> dtos = new ArrayList<>();
        int rankStart = (int) startIndex + 1; // ranks are 1-based
        for (int i = 0; i < resultPlayers.size(); i++) {
            Player p = resultPlayers.get(i);
            dtos.add(PlayerRankingDTO.of(p, rankStart + i));
        }

        return dtos;
    }
}


