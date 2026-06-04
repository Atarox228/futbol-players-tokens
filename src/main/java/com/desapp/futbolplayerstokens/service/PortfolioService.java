package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.PortfolioDTO;

import java.math.BigDecimal;
import java.util.List;

public interface PortfolioService {
    List<PortfolioDTO> getPortfolio(Long userId);
    void updatePosition(Long userId, Long playerId, int qty, BigDecimal price, com.desapp.futbolplayerstokens.modelo.Order.OrderType type);
}

