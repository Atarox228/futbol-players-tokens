package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.repository.OrderRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.QuoteRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;
    private final PlayerRepository playerRepository;
    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            PortfolioRepository portfolioRepository,
                            PlayerRepository playerRepository,
                            QuoteRepository quoteRepository,
                            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.portfolioRepository = portfolioRepository;
        this.playerRepository = playerRepository;
        this.quoteRepository = quoteRepository;
        this.userRepository = userRepository;
    }

    @Override
    public OrderDTO buy(Long userId, Long playerId, int quantity, String idempotencyKey) {
        // check idempotency
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(OrderDTO::toDTO)
                .orElseThrow(() -> new UnsupportedOperationException("not yet implemented"));
    }

    @Override
    public OrderDTO sell(Long userId, Long playerId, int quantity, String idempotencyKey) {
        // check idempotency
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(OrderDTO::toDTO)
                .orElseThrow(() -> new UnsupportedOperationException("not yet implemented"));
    }

    @Override
    public List<OrderDTO> getTransactionsByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(OrderDTO::toDTO).collect(Collectors.toList());
    }
}

