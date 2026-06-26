package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.BuyRequest;
import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.controller.dto.SellRequest;
import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
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
        return orderService.buy(userId, req.getPlayerId(), req.getQuantity(), req.getIdempotencyKey(), req.getMaxPrice());
    }

    @PostMapping("/sell")
    public OrderDTO sell(@RequestBody SellRequest req) {
        Long userId = currentUserId();
        return orderService.sell(userId, req.getPlayerId(), req.getQuantity(), req.getIdempotencyKey(), req.getMinPrice());
    }

    @GetMapping("/transactions")
    public Page<OrderDTO> transactions(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = currentUserId();
        return orderService.getTransactionsByUserId(userId, pageable);
    }

    @GetMapping("/book")
    public Page<OrderDTO> orderBook(
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Order.OrderType t = type != null ? Order.OrderType.valueOf(type.toUpperCase()) : null;
        return orderService.getOrderBook(t, pageable);
    }

    @GetMapping("/pending")
    public Page<OrderDTO> pendingOrders(
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Long userId = currentUserId();
        Order.OrderType t = type != null ? Order.OrderType.valueOf(type.toUpperCase()) : null;
        return orderService.getPendingOrdersByUserId(userId, t, pageable);
    }

    @PostMapping("/sell-all")
    public List<OrderDTO> sellAll() {
        Long userId = currentUserId();
        return orderService.sellAll(userId, "sell-all-" + LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")));
    }

    @GetMapping("/player/{playerId}")
    public List<OrderDTO> ordersByPlayer(@PathVariable Long playerId) {
        return orderService.getOrdersByPlayer(playerId);
    }

    @PostMapping("/{id}/cancel")
    public OrderDTO cancelOrder(@PathVariable Long id) {
        Long userId = currentUserId();
        return orderService.cancelOrder(userId, id);
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username).map(u -> u.getId()).orElseThrow(() -> new RuntimeException("User not found"));
    }

}

