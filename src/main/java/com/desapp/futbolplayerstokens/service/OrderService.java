package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;

import java.util.List;

public interface OrderService {
    OrderDTO buy(Long userId, Long playerId, int quantity, String idempotencyKey);
    OrderDTO sell(Long userId, Long playerId, int quantity, String idempotencyKey);
    List<OrderDTO> getTransactionsByUserId(Long userId);
}

