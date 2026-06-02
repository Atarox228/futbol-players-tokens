package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.exception.InsufficientBalanceException;
import com.desapp.futbolplayerstokens.exception.InsufficientStockException;
import com.desapp.futbolplayerstokens.exception.InsufficientTokensException;
import com.desapp.futbolplayerstokens.exception.ResourceNotFoundException;
import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.OrderRepository;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.service.OrderService;
import com.desapp.futbolplayerstokens.service.PortfolioService;
import com.desapp.futbolplayerstokens.service.QuoteService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final QuoteRepository quoteRepository;
    private final PortfolioService portfolioService;
    private final QuoteService quoteService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            PortfolioRepository portfolioRepository,
                            PlayerRepository playerRepository,
                            UserRepository userRepository,
                            QuoteRepository quoteRepository,
                            PortfolioService portfolioService,
                            @Lazy QuoteService quoteService) {
        this.orderRepository = orderRepository;
        this.portfolioRepository = portfolioRepository;
        this.playerRepository = playerRepository;
        this.userRepository = userRepository;
        this.quoteRepository = quoteRepository;
        this.portfolioService = portfolioService;
        this.quoteService = quoteService;
    }

    @Override
    @Transactional
    public OrderDTO buy(Long userId, Long playerId, int quantity, String idempotencyKey) {
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(OrderDTO::toDTO)
                .orElseGet(() -> createBuyOrder(userId, playerId, quantity, idempotencyKey));
    }

    @Override
    @Transactional
    public OrderDTO sell(Long userId, Long playerId, int quantity, String idempotencyKey) {
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(OrderDTO::toDTO)
                .orElseGet(() -> createSellOrder(userId, playerId, quantity, idempotencyKey));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getTransactionsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return orderRepository.findByUser(user).stream()
                .map(OrderDTO::toDTO)
                .toList();
    }

    private OrderDTO createBuyOrder(Long userId, Long playerId, int quantity, String idempotencyKey) {
        User user = findUser(userId);
        Player player = findPlayer(playerId);

        if (player.getAvailableTokens() < quantity) {
            throw new InsufficientStockException(player.getName(), quantity, player.getAvailableTokens());
        }

        BigDecimal price = quoteService.getCurrentQuote(playerId).getPrice();
        BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));

        if (user.getBalance().compareTo(total) < 0) {
            throw new InsufficientBalanceException(total, user.getBalance());
        }

        user.setBalance(user.getBalance().subtract(total));
        userRepository.save(user);

        player.setAvailableTokens(player.getAvailableTokens() - quantity);
        playerRepository.save(player);

        portfolioService.updatePosition(userId, playerId, quantity, price, Order.OrderType.BUY);

        Order order = Order.builder()
                .user(user)
                .player(player)
                .type(Order.OrderType.BUY)
                .quantity(quantity)
                .priceAtOrder(price)
                .total(total)
                .idempotencyKey(idempotencyKey)
                .build();

        return OrderDTO.toDTO(orderRepository.save(order));
    }

    private OrderDTO createSellOrder(Long userId, Long playerId, int quantity, String idempotencyKey) {
        User user = findUser(userId);
        Player player = findPlayer(playerId);

        Portfolio portfolio = portfolioRepository.findByUserAndPlayer(user, player)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));

        if (portfolio.getTokenQty() < quantity) {
            throw new InsufficientTokensException(quantity, portfolio.getTokenQty());
        }

        BigDecimal price = quoteService.getCurrentQuote(playerId).getPrice();
        BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));

        user.setBalance(user.getBalance().add(total));
        userRepository.save(user);

        player.setAvailableTokens(player.getAvailableTokens() + quantity);
        playerRepository.save(player);

        portfolioService.updatePosition(userId, playerId, quantity, price, Order.OrderType.SELL);

        Order order = Order.builder()
                .user(user)
                .player(player)
                .type(Order.OrderType.SELL)
                .quantity(quantity)
                .priceAtOrder(price)
                .total(total)
                .idempotencyKey(idempotencyKey)
                .build();

        return OrderDTO.toDTO(orderRepository.save(order));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Player findPlayer(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"));
    }
}
