package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.controller.dto.QuoteDTO;
import com.desapp.futbolplayerstokens.security.JwtAuthenticationFilter;
import com.desapp.futbolplayerstokens.security.JwtUtil;
import com.desapp.futbolplayerstokens.service.QuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuoteControllerRESTTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuoteService quoteService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser
    void whenRecalculateAll_thenAccepted() throws Exception {
        doNothing().when(quoteService).recalculateAll(any());

        mockMvc.perform(post("/quotes/recalculate").with(csrf()))
                .andExpect(status().isAccepted());

        verify(quoteService, times(1)).recalculateAll(any());
    }

    @Test
    @WithMockUser
    void whenGetCurrentQuote_thenOk() throws Exception {
        QuoteDTO quoteDTO = QuoteDTO.builder()
                .id(1L)
                .playerId(10L)
                .price(new BigDecimal("1500.00"))
                .timestamp(LocalDateTime.now())
                .strategyId(2L)
                .strategyVersion(1)
                .trigger("MANUAL")
                .build();

        when(quoteService.getCurrentQuote(10L)).thenReturn(quoteDTO);

        mockMvc.perform(get("/quotes/player/10/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.playerId").value(10))
                .andExpect(jsonPath("$.price").value(1500.00))
                .andExpect(jsonPath("$.trigger").value("MANUAL"));

        verify(quoteService, times(1)).getCurrentQuote(10L);
    }

    @Test
    void whenGetCurrentQuoteWithoutAuth_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/quotes/player/1/current"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(quoteService);
    }
}
