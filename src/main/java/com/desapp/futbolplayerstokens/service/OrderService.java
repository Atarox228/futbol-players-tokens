package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.modelo.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {
    OrderDTO buy(Long userId, Long playerId, int quantity, String idempotencyKey, BigDecimal maxPrice);
    OrderDTO sell(Long userId, Long playerId, int quantity, String idempotencyKey, BigDecimal minPrice);
    OrderDTO cancelOrder(Long userId, Long orderId);
    List<OrderDTO> getTransactionsByUserId(Long userId);
    Page<OrderDTO> getTransactionsByUserId(Long userId, Pageable pageable);
    List<OrderDTO> getPendingOrdersByUserId(Long userId);
    Page<OrderDTO> getPendingOrdersByUserId(Long userId, Order.OrderType type, Pageable pageable);
    Page<OrderDTO> getOrderBook(Order.OrderType type, Pageable pageable);
    List<OrderDTO> getAllPendingOrders();
    List<OrderDTO> sellAll(Long userId, String idempotencyKeyPrefix);
    List<OrderDTO> getOrdersByPlayer(Long playerId);
}

