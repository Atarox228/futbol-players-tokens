package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.controller.dto.PortfolioDTO;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.service.OrderService;
import com.desapp.futbolplayerstokens.service.PortfolioService;
import com.desapp.futbolplayerstokens.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Endpoints para consultar información y estado financiero de usuarios")
public class UserControllerREST {

    private final PortfolioService portfolioService;
    private final OrderService orderService;
    private final UserService userService;

    public UserControllerREST(PortfolioService portfolioService, OrderService orderService, UserService userService) {
        this.portfolioService = portfolioService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping("/{id}/portfolio")
    @Operation(summary = "Obtener portfolio paginado", description = "Devuelve la cartera del usuario indicado en formato paginado")
    public Page<PortfolioDTO> getPortfolio(
            @PathVariable("id") Long id,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return portfolioService.getPortfolio(id, pageable);
    }

    @GetMapping("/{id}/portfolio/all")
    @Operation(summary = "Obtener portfolio completo", description = "Devuelve todos los activos del portfolio de un usuario")
    public List<PortfolioDTO> getAllPortfolio(@PathVariable("id") Long id) {
        return portfolioService.getPortfolio(id);
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Obtener transacciones del usuario", description = "Devuelve el historial completo de órdenes del usuario indicado")
    public List<OrderDTO> getTransactions(@PathVariable("id") Long id) {
        return orderService.getTransactionsByUserId(id);
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Obtener balance del usuario", description = "Devuelve el saldo actual y los datos básicos del usuario consultado")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable("id") Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "balance", user.getBalance()
        ));
    }
}

