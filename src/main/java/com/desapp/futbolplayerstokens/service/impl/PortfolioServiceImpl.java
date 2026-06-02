package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PortfolioDTO;
import com.desapp.futbolplayerstokens.exception.ResourceNotFoundException;
import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.service.PortfolioService;
import com.desapp.futbolplayerstokens.service.QuoteService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final QuoteRepository quoteRepository;
    private final QuoteService quoteService;

    public PortfolioServiceImpl(PortfolioRepository portfolioRepository,
                                PlayerRepository playerRepository,
                                UserRepository userRepository,
                                QuoteRepository quoteRepository,
                                @Lazy QuoteService quoteService) {
        this.portfolioRepository = portfolioRepository;
        this.playerRepository = playerRepository;
        this.userRepository = userRepository;
        this.quoteRepository = quoteRepository;
        this.quoteService = quoteService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioDTO> getPortfolio(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return portfolioRepository.findByUser(user).stream()
                .map(portfolio -> {
                    BigDecimal currentPrice = quoteService.getCurrentQuote(portfolio.getPlayer().getId()).getPrice();
                    return PortfolioDTO.of(portfolio, currentPrice);
                })
                .toList();
    }

    @Override
    @Transactional
    public void updatePosition(Long userId, Long playerId, int qty, BigDecimal price, Order.OrderType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"));

        Portfolio portfolio = portfolioRepository.findByUserAndPlayer(user, player).orElse(null);

        if (type == Order.OrderType.BUY) {
            updateBuyPosition(user, player, portfolio, qty, price);
            return;
        }

        updateSellPosition(portfolio, qty);
    }

    private void updateBuyPosition(User user, Player player, Portfolio portfolio, int qty, BigDecimal price) {
        if (portfolio == null) {
            Portfolio newPortfolio = Portfolio.builder()
                    .user(user)
                    .player(player)
                    .tokenQty(qty)
                    .avgBuyPrice(price)
                    .build();
            portfolioRepository.save(newPortfolio);
            return;
        }

        BigDecimal existingCost = portfolio.getAvgBuyPrice().multiply(BigDecimal.valueOf(portfolio.getTokenQty()));
        BigDecimal newCost = price.multiply(BigDecimal.valueOf(qty));
        int newQty = portfolio.getTokenQty() + qty;

        portfolio.setAvgBuyPrice(existingCost.add(newCost)
                .divide(BigDecimal.valueOf(newQty), 8, RoundingMode.HALF_UP));
        portfolio.setTokenQty(newQty);
        portfolioRepository.save(portfolio);
    }

    private void updateSellPosition(Portfolio portfolio, int qty) {
        if (portfolio == null) {
            throw new ResourceNotFoundException("Portfolio not found");
        }

        int newQty = portfolio.getTokenQty() - qty;
        if (newQty == 0) {
            portfolioRepository.delete(portfolio);
            return;
        }

        portfolio.setTokenQty(newQty);
        portfolioRepository.save(portfolio);
    }
}
