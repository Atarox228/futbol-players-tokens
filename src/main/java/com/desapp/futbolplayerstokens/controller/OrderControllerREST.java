package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderControllerREST {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderControllerREST(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping("/buy")
    public OrderDTO buy(@RequestBody BuyRequest req) {
        Long userId = currentUserId();
        return orderService.buy(userId, req.playerId, req.quantity, req.idempotencyKey);
    }

    @PostMapping("/sell")
    public OrderDTO sell(@RequestBody SellRequest req) {
        Long userId = currentUserId();
        return orderService.sell(userId, req.playerId, req.quantity, req.idempotencyKey);
    }

    @GetMapping("/transactions")
    public List<OrderDTO> transactions() {
        Long userId = currentUserId();
        return orderService.getTransactionsByUserId(userId);
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username).map(u -> u.getId()).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public static class BuyRequest {
        public Long playerId;
        public int quantity;
        public String idempotencyKey;
    }

    public static class SellRequest {
        public Long playerId;
        public int quantity;
        public String idempotencyKey;
    }
}

