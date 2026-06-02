package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PortfolioDTO;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.service.PortfolioService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final QuoteRepository quoteRepository;

    public PortfolioServiceImpl(PortfolioRepository portfolioRepository, QuoteRepository quoteRepository) {
        this.portfolioRepository = portfolioRepository;
        this.quoteRepository = quoteRepository;
    }

    @Override
    public List<PortfolioDTO> getPortfolio(Long userId) {
        List<Portfolio> list = portfolioRepository.findByUserId(userId);
        return list.stream().map(PortfolioDTO::toDTO).collect(Collectors.toList());
    }

    @Override
    public void updatePosition(Long userId, Long playerId, int qty, BigDecimal price, com.desapp.futbolplayerstokens.modelo.Order.OrderType type) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}

