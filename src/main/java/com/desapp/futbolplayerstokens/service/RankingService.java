package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.PlayerRankingDTO;

import java.util.List;

public interface RankingService {
    List<PlayerRankingDTO> getRanking(int page, int size);

    void invalidateCache();
}

