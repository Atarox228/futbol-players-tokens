package com.desapp.futbolplayerstokens.service.impl;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.exception.InsufficientBalanceException;
import com.desapp.futbolplayerstokens.exception.InsufficientTokensException;
import com.desapp.futbolplayerstokens.exception.ResourceNotFoundException;
import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.OrderRepository;
import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import com.desapp.futbolplayerstokens.repository.PortfolioRepository;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.service.OrderService;
import com.desapp.futbolplayerstokens.service.PortfolioService;
import com.desapp.futbolplayerstokens.service.QuoteService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PortfolioRepository portfolioRepository;
    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final PortfolioService portfolioService;
    private final QuoteService quoteService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            PortfolioRepository portfolioRepository,
                            PlayerRepository playerRepository,
                            UserRepository userRepository,
                            PortfolioService portfolioService,
                            @Lazy QuoteService quoteService) {
        this.orderRepository = orderRepository;
        this.portfolioRepository = portfolioRepository;
        this.playerRepository = playerRepository;
        this.userRepository = userRepository;
        this.portfolioService = portfolioService;
        this.quoteService = quoteService;
    }

    @Override
    @Transactional
    public OrderDTO buy(Long userId, Long playerId, int quantity, String idempotencyKey, BigDecimal maxPrice) {
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(OrderDTO::toDTO)
                .orElseGet(() -> createBuyOrder(userId, playerId, quantity, idempotencyKey, maxPrice));
    }

    @Override
    @Transactional
    public OrderDTO sell(Long userId, Long playerId, int quantity, String idempotencyKey, BigDecimal minPrice) {
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(OrderDTO::toDTO)
                .orElseGet(() -> createSellOrder(userId, playerId, quantity, idempotencyKey, minPrice));
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to this user");
        }
        if (order.getStatus() == Order.OrderStatus.FILLED || order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order cannot be cancelled");
        }

        int remaining = order.getRemainingQuantity();

        if (order.getType() == Order.OrderType.BUY) {
            BigDecimal refund = order.getPriceAtOrder().multiply(BigDecimal.valueOf(remaining));
            User user = order.getUser();
            user.setBalance(user.getBalance().add(refund));
            userRepository.save(user);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setRemainingQuantity(0);
        return OrderDTO.toDTO(orderRepository.save(order));
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

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getTransactionsByUserId(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return orderRepository.findByUser(user, pageable).map(OrderDTO::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getPendingOrdersByUserId(Long userId) {
        return getPendingOrdersByUserId(userId, null, Pageable.unpaged()).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getPendingOrdersByUserId(Long userId, Order.OrderType type, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return (type == null
                ? orderRepository.findByUserAndStatus(user, Order.OrderStatus.PENDING, pageable)
                : orderRepository.findByUserAndStatusAndType(user, Order.OrderStatus.PENDING, type, pageable))
                .map(OrderDTO::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> getOrderBook(Order.OrderType type, Pageable pageable) {
        return (type == null
                ? orderRepository.findByStatus(Order.OrderStatus.PENDING, pageable)
                : orderRepository.findByStatusAndType(Order.OrderStatus.PENDING, type, pageable))
                .map(OrderDTO::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllPendingOrders() {
        return orderRepository.findByStatus(Order.OrderStatus.PENDING).stream()
                .map(OrderDTO::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public List<OrderDTO> sellAll(Long userId, String idempotencyKeyPrefix) {
        User user = findUser(userId);
        List<Portfolio> portfolio = portfolioRepository.findByUser(user);
        List<OrderDTO> orders = new ArrayList<>();
        for (Portfolio p : portfolio) {
            if (p.getTokenQty() <= 0) continue;
            String key = idempotencyKeyPrefix + "-" + p.getPlayer().getId();
            OrderDTO order = sell(userId, p.getPlayer().getId(), p.getTokenQty(), key, null);
            orders.add(order);
        }
        return orders;
    }

    private OrderDTO createBuyOrder(Long userId, Long playerId, int quantity, String idempotencyKey, BigDecimal maxPrice) {
        User user = findUser(userId);
        Player player = findPlayer(playerId);

        if (maxPrice == null) {
            maxPrice = quoteService.getCurrentQuote(playerId).getPrice();
        }

        BigDecimal totalReserved = maxPrice.multiply(BigDecimal.valueOf(quantity));
        if (user.getBalance().compareTo(totalReserved) < 0) {
            throw new InsufficientBalanceException(totalReserved, user.getBalance());
        }

        user.setBalance(user.getBalance().subtract(totalReserved));
        userRepository.save(user);

        Order order = Order.builder()
                .user(user)
                .player(player)
                .type(Order.OrderType.BUY)
                .quantity(quantity)
                .priceAtOrder(maxPrice)
                .total(totalReserved)
                .idempotencyKey(idempotencyKey)
                .status(Order.OrderStatus.PENDING)
                .remainingQuantity(quantity)
                .build();
        order = orderRepository.save(order);

        matchBuyOrder(order);

        return OrderDTO.toDTO(orderRepository.findById(order.getId()).orElse(order));
    }

    private OrderDTO createSellOrder(Long userId, Long playerId, int quantity, String idempotencyKey, BigDecimal minPrice) {
        User user = findUser(userId);
        Player player = findPlayer(playerId);

        if (minPrice == null) {
            minPrice = quoteService.getCurrentQuote(playerId).getPrice();
        }

        Portfolio portfolio = portfolioRepository.findByUserAndPlayer(user, player)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));

        if (portfolio.getTokenQty() < quantity) {
            throw new InsufficientTokensException(quantity, portfolio.getTokenQty());
        }

        Order order = Order.builder()
                .user(user)
                .player(player)
                .type(Order.OrderType.SELL)
                .quantity(quantity)
                .priceAtOrder(minPrice)
                .total(minPrice.multiply(BigDecimal.valueOf(quantity)))
                .idempotencyKey(idempotencyKey)
                .status(Order.OrderStatus.PENDING)
                .remainingQuantity(quantity)
                .build();
        order = orderRepository.save(order);

        matchSellOrder(order);

        return OrderDTO.toDTO(orderRepository.findById(order.getId()).orElse(order));
    }

    private void matchBuyOrder(Order buyOrder) {
        List<Order> sellOrders = orderRepository.findPendingSellOrdersForBuy(
                buyOrder.getPlayer(), buyOrder.getPriceAtOrder());

        for (Order sellOrder : sellOrders) {
            if (buyOrder.getRemainingQuantity() <= 0) break;
            if (sellOrder.getRemainingQuantity() <= 0) continue;

            int matchedQty = Math.min(buyOrder.getRemainingQuantity(), sellOrder.getRemainingQuantity());
            BigDecimal sellPrice = sellOrder.getPriceAtOrder();
            BigDecimal cost = sellPrice.multiply(BigDecimal.valueOf(matchedQty));

            portfolioService.transferTokens(sellOrder.getUser().getId(), buyOrder.getUser().getId(),
                    buyOrder.getPlayer().getId(), matchedQty);

            User seller = sellOrder.getUser();
            seller.setBalance(seller.getBalance().add(cost));
            userRepository.save(seller);

            if (buyOrder.getPriceAtOrder().compareTo(sellPrice) > 0) {
                BigDecimal refund = buyOrder.getPriceAtOrder().subtract(sellPrice)
                        .multiply(BigDecimal.valueOf(matchedQty));
                User buyer = buyOrder.getUser();
                buyer.setBalance(buyer.getBalance().add(refund));
                userRepository.save(buyer);
            }

            sellOrder.setRemainingQuantity(sellOrder.getRemainingQuantity() - matchedQty);
            if (sellOrder.getRemainingQuantity() == 0) {
                sellOrder.setStatus(Order.OrderStatus.FILLED);
            } else {
                sellOrder.setStatus(Order.OrderStatus.PARTIALLY_FILLED);
            }
            orderRepository.save(sellOrder);

            buyOrder.setRemainingQuantity(buyOrder.getRemainingQuantity() - matchedQty);
        }

        if (buyOrder.getRemainingQuantity() == 0) {
            buyOrder.setStatus(Order.OrderStatus.FILLED);
        }
        orderRepository.save(buyOrder);
    }

    private void matchSellOrder(Order sellOrder) {
        List<Order> buyOrders = orderRepository.findPendingBuyOrdersForSell(
                sellOrder.getPlayer(), sellOrder.getPriceAtOrder());

        for (Order buyOrder : buyOrders) {
            if (sellOrder.getRemainingQuantity() <= 0) break;
            if (buyOrder.getRemainingQuantity() <= 0) continue;

            int matchedQty = Math.min(sellOrder.getRemainingQuantity(), buyOrder.getRemainingQuantity());
            BigDecimal sellPrice = sellOrder.getPriceAtOrder();
            BigDecimal cost = sellPrice.multiply(BigDecimal.valueOf(matchedQty));

            portfolioService.transferTokens(sellOrder.getUser().getId(), buyOrder.getUser().getId(),
                    sellOrder.getPlayer().getId(), matchedQty);

            User seller = sellOrder.getUser();
            seller.setBalance(seller.getBalance().add(cost));
            userRepository.save(seller);

            if (buyOrder.getPriceAtOrder().compareTo(sellPrice) > 0) {
                BigDecimal refund = buyOrder.getPriceAtOrder().subtract(sellPrice)
                        .multiply(BigDecimal.valueOf(matchedQty));
                User buyer = buyOrder.getUser();
                buyer.setBalance(buyer.getBalance().add(refund));
                userRepository.save(buyer);
            }

            buyOrder.setRemainingQuantity(buyOrder.getRemainingQuantity() - matchedQty);
            if (buyOrder.getRemainingQuantity() == 0) {
                buyOrder.setStatus(Order.OrderStatus.FILLED);
            } else {
                buyOrder.setStatus(Order.OrderStatus.PARTIALLY_FILLED);
            }
            orderRepository.save(buyOrder);

            sellOrder.setRemainingQuantity(sellOrder.getRemainingQuantity() - matchedQty);
        }

        if (sellOrder.getRemainingQuantity() == 0) {
            sellOrder.setStatus(Order.OrderStatus.FILLED);
        } else if (sellOrder.getRemainingQuantity() < sellOrder.getQuantity()) {
            sellOrder.setStatus(Order.OrderStatus.PARTIALLY_FILLED);
        }
        orderRepository.save(sellOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByPlayer(Long playerId) {
        Player player = findPlayer(playerId);
        return orderRepository.findByPlayerAndStatusIn(player,
                List.of(Order.OrderStatus.PENDING, Order.OrderStatus.PARTIALLY_FILLED))
                .stream()
                .map(OrderDTO::toDTO)
                .toList();
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
