package com.desapp.futbolplayerstokens.e2e;

import com.desapp.futbolplayerstokens.modelo.Order;
import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerE2ETest extends E2EBaseTest {

    private Player player;
    private User buyer;

    @BeforeEach
    void seedOrderData() {
        player = seedPlayer("Kylian Mbappe", "PSG", "LIGUE_1", "Forward", new BigDecimal("97.00000000"), 20, 20);
        seedQuote(player, new BigDecimal("100.00000000"));
        buyer = seedUser("buyer_user", "buyer_user@example.com", "BuyerPassword123!", new BigDecimal("1000.00000000"));
    }

    @Test
    void buySuccessReservesBalanceAndReturnsOrder() throws Exception {
        mockMvc.perform(post("/orders/buy")
                        .header("Authorization", authHeaderValue())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "playerId", player.getId(),
                                "quantity", 3,
                                "idempotencyKey", "buy-success-1"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(authenticatedUser.getId().intValue()))
                .andExpect(jsonPath("$.playerId").value(player.getId().intValue()))
                .andExpect(jsonPath("$.playerName").value("Kylian Mbappe"))
                .andExpect(jsonPath("$.type").value("BUY"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.priceAtOrder").isNumber())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.status").value("PENDING"));

        User updatedUser = userRepository.findById(authenticatedUser.getId()).orElseThrow();
        assertEquals(new BigDecimal("700.00000000"), updatedUser.getBalance());
    }

    @Test
    void buyInsufficientBalanceReturnsConflict() throws Exception {
        authenticatedUser.setBalance(new BigDecimal("50.00000000"));
        userRepository.save(authenticatedUser);

        mockMvc.perform(post("/orders/buy")
                        .header("Authorization", authHeaderValue())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "playerId", player.getId(),
                                "quantity", 1,
                                "idempotencyKey", "buy-conflict-1"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void sellSuccessTransfersTokensAndReturnsOrder() throws Exception {
        seedPortfolio(authenticatedUser, player, 5, new BigDecimal("80.00000000"));
        seedOrder(buyer, player, Order.OrderType.BUY, 2, new BigDecimal("120.00000000"), "buy-open-1", Order.OrderStatus.PENDING, 2);

        mockMvc.perform(post("/orders/sell")
                        .header("Authorization", authHeaderValue())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "playerId", player.getId(),
                                "quantity", 2,
                                "idempotencyKey", "sell-success-1"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(authenticatedUser.getId().intValue()))
                .andExpect(jsonPath("$.playerId").value(player.getId().intValue()))
                .andExpect(jsonPath("$.type").value("SELL"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("FILLED"));

        Portfolio sellerPortfolio = portfolioRepository.findByUserAndPlayer(authenticatedUser, player).orElseThrow();
        assertEquals(3, sellerPortfolio.getTokenQty());
    }

    @Test
    void sellWithoutTokensReturnsConflict() throws Exception {
        seedPortfolio(authenticatedUser, player, 0, new BigDecimal("0.00000000"));

        mockMvc.perform(post("/orders/sell")
                        .header("Authorization", authHeaderValue())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "playerId", player.getId(),
                                "quantity", 1,
                                "idempotencyKey", "sell-conflict-1"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getTransactionsReturnsCurrentUserOrders() throws Exception {
        mockMvc.perform(post("/orders/buy")
                        .header("Authorization", authHeaderValue())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "playerId", player.getId(),
                                "quantity", 1,
                                "idempotencyKey", "buy-transactions-1"
                        ))))
                .andExpect(status().isOk());

        seedPortfolio(authenticatedUser, player, 2, new BigDecimal("80.00000000"));
        mockMvc.perform(post("/orders/sell")
                        .header("Authorization", authHeaderValue())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "playerId", player.getId(),
                                "quantity", 1,
                                "idempotencyKey", "sell-transactions-1"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/orders/transactions")
                        .header("Authorization", authHeaderValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andExpect(jsonPath("$.content[0].type").isString())
                .andExpect(jsonPath("$.content[0].playerId").value(player.getId().intValue()));
    }
}