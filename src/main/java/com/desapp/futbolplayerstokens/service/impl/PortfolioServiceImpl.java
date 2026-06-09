package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.PortfolioDTO;
import com.desapp.futbolplayerstokens.exception.InsufficientTokensException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final QuoteRepository quoteRepository;
    private final QuoteService quoteService;

    private static final String SINUSUARIO = "User not found";

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
                .orElseThrow(() -> new ResourceNotFoundException(SINUSUARIO));

        return portfolioRepository.findByUser(user).stream()
                .map(portfolio -> {
                    BigDecimal currentPrice = quoteService.getCurrentQuote(portfolio.getPlayer().getId()).getPrice();
                    return PortfolioDTO.of(portfolio, currentPrice);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PortfolioDTO> getPortfolio(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(SINUSUARIO));

        return portfolioRepository.findByUser(user, pageable)
                .map(portfolio -> {
                    BigDecimal currentPrice = quoteService.getCurrentQuote(portfolio.getPlayer().getId()).getPrice();
                    return PortfolioDTO.of(portfolio, currentPrice);
                });
    }

    @Override
    @Transactional
    public void updatePosition(Long userId, Long playerId, int qty, BigDecimal price, Order.OrderType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(SINUSUARIO));
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

    @Override
    @Transactional
    public void transferTokens(Long fromUserId, Long toUserId, Long playerId, int quantity, BigDecimal buyPrice) {
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new ResourceNotFoundException("From user not found"));
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new ResourceNotFoundException("To user not found"));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"));

        Portfolio fromPortfolio = portfolioRepository.findByUserAndPlayer(fromUser, player)
                .orElseThrow(() -> new ResourceNotFoundException("From user has no portfolio for this player"));

        if (fromPortfolio.getTokenQty() < quantity) {
            throw new InsufficientTokensException(quantity, fromPortfolio.getTokenQty());
        }

        updateSellPosition(fromPortfolio, quantity);

        BigDecimal price = Objects.requireNonNullElseGet(buyPrice, () -> BigDecimal.ZERO);
        BigDecimal newCost = price.multiply(BigDecimal.valueOf(quantity));

        Portfolio toPortfolio = portfolioRepository.findByUserAndPlayer(toUser, player).orElse(null);
        if (toPortfolio == null) {
            Portfolio newPortfolio = Portfolio.builder()
                    .user(toUser)
                    .player(player)
                    .tokenQty(quantity)
                    .avgBuyPrice(price)
                    .build();
            portfolioRepository.save(newPortfolio);
        } else {
            BigDecimal existingCost = toPortfolio.getAvgBuyPrice()
                    .multiply(BigDecimal.valueOf(toPortfolio.getTokenQty()));
            int newQty = toPortfolio.getTokenQty() + quantity;
            toPortfolio.setAvgBuyPrice(existingCost.add(newCost)
                    .divide(BigDecimal.valueOf(newQty), 8, RoundingMode.HALF_UP));
            toPortfolio.setTokenQty(newQty);
            portfolioRepository.save(toPortfolio);
        }
    }
}
