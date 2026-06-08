package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.PortfolioDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface PortfolioService {
    List<PortfolioDTO> getPortfolio(Long userId);
    Page<PortfolioDTO> getPortfolio(Long userId, Pageable pageable);
    void updatePosition(Long userId, Long playerId, int qty, BigDecimal price, com.desapp.futbolplayerstokens.modelo.Order.OrderType type);
    void transferTokens(Long fromUserId, Long toUserId, Long playerId, int quantity);
}

