package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.controller.dto.PortfolioDTO;
import com.desapp.futbolplayerstokens.service.OrderService;
import com.desapp.futbolplayerstokens.service.PortfolioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserControllerREST {

    private final PortfolioService portfolioService;
    private final OrderService orderService;

    public UserControllerREST(PortfolioService portfolioService, OrderService orderService) {
        this.portfolioService = portfolioService;
        this.orderService = orderService;
    }

    @GetMapping("/{id}/portfolio")
    public List<PortfolioDTO> getPortfolio(@PathVariable("id") Long id) {
        return portfolioService.getPortfolio(id);
    }

    @GetMapping("/{id}/transactions")
    public List<OrderDTO> getTransactions(@PathVariable("id") Long id) {
        return orderService.getTransactionsByUserId(id);
    }
}

