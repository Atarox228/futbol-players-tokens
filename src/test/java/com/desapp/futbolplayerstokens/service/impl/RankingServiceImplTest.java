package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PlayerRankingDTO;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.service.ActiveStrategyService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceImplTest {

    private static final String CACHE_KEY_PAGE_0_SIZE_2 = "ranking:0:2";

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private ActiveStrategyService activeStrategyService;

    @Mock
    private RedisTemplate<String, List<PlayerRankingDTO>> redisTemplate;

    @Mock
    private ValueOperations<String, List<PlayerRankingDTO>> valueOperations;

    private RankingServiceImpl rankingService;

    private Player p1, p2, p3, p4;

    @BeforeEach
    void setUp() {
        p1 = Player.builder().id(1L).name("A").score(new BigDecimal("200")).build();
        p2 = Player.builder().id(2L).name("B").score(new BigDecimal("150")).build();
        p3 = Player.builder().id(3L).name("C").score(null).build();
        p4 = Player.builder().id(4L).name("D").score(null).build();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rankingService = new RankingServiceImpl(playerRepository, activeStrategyService, redisTemplate, 5);
    }

    @Test
    void testRanking_cacheHit_returnsCachedRankingWithoutDatabase() {
        List<PlayerRankingDTO> cached = List.of(
                PlayerRankingDTO.of(p1, 1),
                PlayerRankingDTO.of(p2, 2)
        );
        when(valueOperations.get(CACHE_KEY_PAGE_0_SIZE_2)).thenReturn(cached);

        List<PlayerRankingDTO> list = rankingService.getRanking(0, 2);

        assertEquals(cached, list);
        verify(playerRepository, never()).countByScoreIsNotNull();
        verify(valueOperations, never()).set(anyString(), anyList(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testRanking_cacheMiss_queriesDatabaseAndStoresResult() {
        when(valueOperations.get(CACHE_KEY_PAGE_0_SIZE_2)).thenReturn(null);
        when(playerRepository.countByScoreIsNotNull()).thenReturn(2L);
        when(playerRepository.count()).thenReturn(4L);
        when(playerRepository.findByScoreNotNullOrdered(org.springframework.data.domain.PageRequest.of(0,2)))
                .thenReturn(List.of(p1, p2));

        List<PlayerRankingDTO> list = rankingService.getRanking(0, 2);

        assertEquals(2, list.size());
        assertEquals("1", list.get(0).getPlayerId());
        assertEquals(1, list.get(0).getRank());
        assertEquals(new BigDecimal("200"), list.get(0).getScore());

        assertEquals("2", list.get(1).getPlayerId());
        assertEquals(2, list.get(1).getRank());
    verify(valueOperations).set(eq(CACHE_KEY_PAGE_0_SIZE_2), eq(list), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void testRanking_PageCrossesToNulls() {
        // nonNullCount =2, total 4, request page=1,size=2 -> startIndex=2 -> should return p3 and p4 with ranks 3 and 4
        when(playerRepository.countByScoreIsNotNull()).thenReturn(2L);
        when(playerRepository.count()).thenReturn(4L);
        when(playerRepository.findByScoreNotNullOrdered(org.springframework.data.domain.PageRequest.of(0,2)))
                .thenReturn(List.of(p1, p2));
        when(playerRepository.findByScoreNullOrdered(org.springframework.data.domain.PageRequest.of(0,2)))
                .thenReturn(List.of(p3, p4));
    when(valueOperations.get("ranking:1:2")).thenReturn(null);

        List<PlayerRankingDTO> list = rankingService.getRanking(1, 2);

        assertEquals(2, list.size());
        assertEquals("3", list.get(0).getPlayerId());
        assertEquals(3, list.get(0).getRank());
        assertNull(list.get(0).getScore());

        assertEquals("4", list.get(1).getPlayerId());
        assertEquals(4, list.get(1).getRank());
    }

    @Test
    void invalidateCache_deletesRankingKeys() {
        when(redisTemplate.keys("ranking:*")).thenReturn(Set.of("ranking:0:2", "ranking:1:2"));

        rankingService.invalidateCache();

        verify(redisTemplate).delete(Set.of("ranking:0:2", "ranking:1:2"));
    }
}

