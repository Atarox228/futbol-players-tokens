package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.OrderDTO;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import com.desapp.futbolplayerstokens.security.JwtAuthenticationFilter;
import com.desapp.futbolplayerstokens.security.JwtUtil;
import com.desapp.futbolplayerstokens.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderControllerREST.class)
@MockitoBean(types = OrderService.class)
@MockitoBean(types = UserRepository.class)
@MockitoBean(types = JwtAuthenticationFilter.class)
@MockitoBean(types = UserDetailsService.class)
@MockitoBean(types = JwtUtil.class)
class OrderControllerRESTTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "john")
    void whenBuy_thenReturnOrderDTO() throws Exception {
        User user = User.builder().id(10L).username("john").build();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        OrderDTO orderDTO = OrderDTO.builder()
                .id(100L)
                .userId(10L)
                .playerId(20L)
                .playerName("Test Player")
                .type("BUY")
                .quantity(2)
                .priceAtOrder(new BigDecimal("500.00"))
                .total(new BigDecimal("1000.00"))
                .idempotencyKey("abcd1234")
                .createdAt(LocalDateTime.now())
                .build();

        when(orderService.buy(eq(10L), eq(20L), eq(2), eq("abcd1234"))).thenReturn(orderDTO);

        String payload = "{\"playerId\":20,\"quantity\":2,\"idempotencyKey\":\"abcd1234\"}";

        mockMvc.perform(post("/orders/buy").with(csrf()).contentType("application/json").content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.playerId").value(20))
                .andExpect(jsonPath("$.quantity").value(2));

        verify(orderService, times(1)).buy(eq(10L), eq(20L), eq(2), eq("abcd1234"));
    }

    @Test
    @WithMockUser(username = "john")
    void whenTransactions_thenReturnList() throws Exception {
        User user = User.builder().id(10L).username("john").build();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        OrderDTO dto = OrderDTO.builder()
                .id(101L)
                .userId(10L)
                .playerId(22L)
                .playerName("Player B")
                .type("SELL")
                .quantity(1)
                .priceAtOrder(new BigDecimal("750.00"))
                .total(new BigDecimal("750.00"))
                .idempotencyKey("key2")
                .createdAt(LocalDateTime.now())
                .build();

        when(orderService.getTransactionsByUserId(10L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/orders/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].type").value("SELL"))
                .andExpect(jsonPath("$[0].playerName").value("Player B"));

        verify(orderService, times(1)).getTransactionsByUserId(10L);
    }

    @Test
    void whenTransactionsWithoutAuth_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/orders/transactions"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
        verifyNoInteractions(userRepository);
    }
}
