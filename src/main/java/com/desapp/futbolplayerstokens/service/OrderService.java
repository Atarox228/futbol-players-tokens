package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {
    OrderDTO buy(Long userId, Long playerId, int quantity, String idempotencyKey, BigDecimal maxPrice);
    OrderDTO sell(Long userId, Long playerId, int quantity, String idempotencyKey, BigDecimal minPrice);
    OrderDTO cancelOrder(Long userId, Long orderId);
    List<OrderDTO> getTransactionsByUserId(Long userId);
    List<OrderDTO> getPendingOrdersByUserId(Long userId);
    List<OrderDTO> getAllPendingOrders();
}

