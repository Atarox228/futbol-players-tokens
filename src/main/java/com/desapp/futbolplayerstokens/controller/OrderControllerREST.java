package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.BuyRequest;
import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.controller.dto.SellRequest;
import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Orders", description = "Endpoints para comprar, vender y consultar operaciones de un usuario")
public class OrderControllerREST {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderControllerREST(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping("/buy")
    @Operation(summary = "Comprar tokens", description = "Registra una orden de compra de tokens para el usuario autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden de compra creada"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o compra rechazada")
    })
    public OrderDTO buy(@RequestBody BuyRequest req) {
        Long userId = currentUserId();
        return orderService.buy(userId, req.getPlayerId(), req.getQuantity(), req.getIdempotencyKey(), req.getMaxPrice());
    }

    @PostMapping("/sell")
    @Operation(summary = "Vender tokens", description = "Registra una orden de venta de tokens para el usuario autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden de venta creada"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o venta rechazada")
    })
    public OrderDTO sell(@RequestBody SellRequest req) {
        Long userId = currentUserId();
        return orderService.sell(userId, req.getPlayerId(), req.getQuantity(), req.getIdempotencyKey(), req.getMinPrice());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Obtener transacciones", description = "Devuelve el historial paginado de órdenes del usuario autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial de transacciones obtenido")
    })
    public Page<OrderDTO> transactions(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = currentUserId();
        return orderService.getTransactionsByUserId(userId, pageable);
    }

    @GetMapping("/book")
    @Operation(summary = "Obtener book de órdenes", description = "Devuelve el libro de órdenes filtrado opcionalmente por tipo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Book de órdenes obtenido")
    })
    public Page<OrderDTO> orderBook(
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Order.OrderType t = type != null ? Order.OrderType.valueOf(type.toUpperCase()) : null;
        return orderService.getOrderBook(t, pageable);
    }

    @GetMapping("/pending")
    @Operation(summary = "Obtener órdenes pendientes", description = "Devuelve las órdenes pendientes del usuario autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Órdenes pendientes obtenidas")
    })
    public Page<OrderDTO> pendingOrders(
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Long userId = currentUserId();
        Order.OrderType t = type != null ? Order.OrderType.valueOf(type.toUpperCase()) : null;
        return orderService.getPendingOrdersByUserId(userId, t, pageable);
    }

    @PostMapping("/sell-all")
    @Operation(summary = "Vender todo", description = "Crea órdenes de venta para liquidar toda la posición del usuario autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Venta total procesada")
    })
    public List<OrderDTO> sellAll() {
        Long userId = currentUserId();
        return orderService.sellAll(userId, "sell-all-" + LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")));
    }

    @GetMapping("/player/{playerId}")
    @Operation(summary = "Obtener órdenes por jugador", description = "Devuelve todas las órdenes asociadas a un jugador específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Órdenes del jugador obtenidas")
    })
    public List<OrderDTO> ordersByPlayer(@PathVariable Long playerId) {
        return orderService.getOrdersByPlayer(playerId);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar orden", description = "Cancela una orden del usuario autenticado usando su identificador")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden cancelada"),
        @ApiResponse(responseCode = "400", description = "La orden no pudo cancelarse")
    })
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

