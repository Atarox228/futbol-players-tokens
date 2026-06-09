package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.controller.dto.PortfolioDTO;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.service.OrderService;
import com.desapp.futbolplayerstokens.service.PortfolioService;
import com.desapp.futbolplayerstokens.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerRESTTest {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserControllerREST userController;

    @Test
    void getPortfolio_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<PortfolioDTO> page = new PageImpl<>(List.of(PortfolioDTO.builder().playerId(1L).build()));
        when(portfolioService.getPortfolio(1L, pageable)).thenReturn(page);

        Page<PortfolioDTO> result = userController.getPortfolio(1L, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getPlayerId());
    }

    @Test
    void getPortfolio_empty() {
        Pageable pageable = PageRequest.of(0, 20);
        when(portfolioService.getPortfolio(1L, pageable)).thenReturn(Page.empty());

        Page<PortfolioDTO> result = userController.getPortfolio(1L, pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void getTransactions_returnsList() {
        when(orderService.getTransactionsByUserId(1L)).thenReturn(List.of(OrderDTO.builder().id(1L).build()));

        List<OrderDTO> result = userController.getTransactions(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getTransactions_empty() {
        when(orderService.getTransactionsByUserId(1L)).thenReturn(List.of());

        List<OrderDTO> result = userController.getTransactions(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getBalance_returnsBalanceInfo() {
        User user = User.builder().id(1L).username("testuser").balance(new BigDecimal("5000")).build();
        when(userService.findById(1L)).thenReturn(user);

        ResponseEntity<Map<String, Object>> result = userController.getBalance(1L);

        assertEquals(200, result.getStatusCode().value());
        Map<String, Object> body = result.getBody();
        assertNotNull(body);
        assertEquals(1L, body.get("userId"));
        assertEquals("testuser", body.get("username"));
        assertEquals(new BigDecimal("5000"), body.get("balance"));
    }

    @Test
    void getBalance_zeroBalance() {
        User user = User.builder().id(2L).username("pooruser").balance(BigDecimal.ZERO).build();
        when(userService.findById(2L)).thenReturn(user);

        ResponseEntity<Map<String, Object>> result = userController.getBalance(2L);

        Map<String, Object> body = result.getBody();
        assertEquals(BigDecimal.ZERO, body.get("balance"));
    }
}
