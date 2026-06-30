package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PlayerRankingDTO;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.service.RankingService;
import com.desapp.futbolplayerstokens.service.ActiveStrategyService;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RankingServiceImpl implements RankingService {

    private static final String CACHE_KEY_PREFIX = "ranking:";

    private final PlayerRepository playerRepository;
    private final ActiveStrategyService activeStrategyService;
    private final RedisTemplate<String, List<PlayerRankingDTO>> redisTemplate;
    private final int rankingCacheTtlMinutes;

    public RankingServiceImpl(PlayerRepository playerRepository,
                              ActiveStrategyService activeStrategyService,
                              @Nullable RedisTemplate<String, List<PlayerRankingDTO>> redisTemplate,
                              @Value("${app.cache.ranking-ttl-minutes:5}") int rankingCacheTtlMinutes) {
        this.playerRepository = playerRepository;
        this.activeStrategyService = activeStrategyService;
        this.redisTemplate = redisTemplate;
        this.rankingCacheTtlMinutes = rankingCacheTtlMinutes;
    }

    @Override
    public List<PlayerRankingDTO> getRanking(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;

        String cacheKey = buildCacheKey(page, size);
        List<PlayerRankingDTO> cachedRanking = getCachedRanking(cacheKey);
        if (cachedRanking != null) {
            return cachedRanking;
        }

        List<PlayerRankingDTO> ranking = loadRankingFromDatabase(page, size);
        cacheRanking(cacheKey, ranking);
        return ranking;
    }

    @Override
    public void invalidateCache() {
        if (redisTemplate == null) {
            return;
        }

        Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private List<PlayerRankingDTO> getCachedRanking(String cacheKey) {
        if (redisTemplate == null) {
            return null;
        }

        return redisTemplate.opsForValue().get(cacheKey);
    }

    private void cacheRanking(String cacheKey, List<PlayerRankingDTO> ranking) {
        if (redisTemplate == null) {
            return;
        }

        redisTemplate.opsForValue().set(cacheKey, ranking, rankingCacheTtlMinutes, TimeUnit.MINUTES);
    }

    private String buildCacheKey(int page, int size) {
        return CACHE_KEY_PREFIX + page + ":" + size;
    }

    private List<PlayerRankingDTO> loadRankingFromDatabase(int page, int size) {
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


