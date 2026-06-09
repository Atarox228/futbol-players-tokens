package com.desapp.futbolplayerstokens.controller.dto;

import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioDTOTest {

    @Test
    void of_createsDTOWithCalculatedValues() {
        Player player = Player.builder().id(1L).name("Messi").build();
        Portfolio portfolio = Portfolio.builder()
                .player(player)
                .tokenQty(10)
                .avgBuyPrice(new BigDecimal("50"))
                .build();

        PortfolioDTO dto = PortfolioDTO.of(portfolio, new BigDecimal("75"));

        assertEquals(1L, dto.getPlayerId());
        assertEquals("Messi", dto.getPlayerName());
        assertEquals(10, dto.getTokenQty());
        assertEquals(new BigDecimal("50"), dto.getAvgBuyPrice());
        assertEquals(new BigDecimal("75"), dto.getCurrentPrice());
        assertEquals(new BigDecimal("750.00000000"), dto.getCurrentValue());
        assertEquals(new BigDecimal("250.00000000"), dto.getProfitLoss());
    }

    @Test
    void of_zeroQuantity() {
        Player player = Player.builder().id(2L).name("Ronaldo").build();
        Portfolio portfolio = Portfolio.builder()
                .player(player)
                .tokenQty(0)
                .avgBuyPrice(new BigDecimal("100"))
                .build();

        PortfolioDTO dto = PortfolioDTO.of(portfolio, new BigDecimal("200"));

        assertEquals(BigDecimal.ZERO.setScale(8), dto.getCurrentValue());
        assertEquals(BigDecimal.ZERO.setScale(8), dto.getProfitLoss());
    }

    @Test
    void of_lossScenario() {
        Player player = Player.builder().id(3L).name("Neymar").build();
        Portfolio portfolio = Portfolio.builder()
                .player(player)
                .tokenQty(5)
                .avgBuyPrice(new BigDecimal("100"))
                .build();

        PortfolioDTO dto = PortfolioDTO.of(portfolio, new BigDecimal("60"));

        assertEquals(new BigDecimal("300.00000000"), dto.getCurrentValue());
        assertEquals(new BigDecimal("-200.00000000"), dto.getProfitLoss());
    }

    @Test
    void of_zeroPrice() {
        Player player = Player.builder().id(4L).name("Mbappe").build();
        Portfolio portfolio = Portfolio.builder()
                .player(player)
                .tokenQty(8)
                .avgBuyPrice(new BigDecimal("50"))
                .build();

        PortfolioDTO dto = PortfolioDTO.of(portfolio, BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO.setScale(8), dto.getCurrentValue());
        assertEquals(new BigDecimal("-400.00000000"), dto.getProfitLoss());
    }

    @Test
    void of_zeroAvgBuyPrice() {
        Player player = Player.builder().id(5L).name("Haaland").build();
        Portfolio portfolio = Portfolio.builder()
                .player(player)
                .tokenQty(3)
                .avgBuyPrice(BigDecimal.ZERO)
                .build();

        PortfolioDTO dto = PortfolioDTO.of(portfolio, new BigDecimal("90"));

        assertEquals(new BigDecimal("270.00000000"), dto.getCurrentValue());
        assertEquals(new BigDecimal("270.00000000"), dto.getProfitLoss());
    }

    @Test
    void of_singleToken() {
        Player player = Player.builder().id(6L).name("Salah").build();
        Portfolio portfolio = Portfolio.builder()
                .player(player)
                .tokenQty(1)
                .avgBuyPrice(new BigDecimal("88.50"))
                .build();

        PortfolioDTO dto = PortfolioDTO.of(portfolio, new BigDecimal("100.25"));

        assertEquals(new BigDecimal("100.25000000"), dto.getCurrentValue());
        assertEquals(new BigDecimal("11.75000000"), dto.getProfitLoss());
    }

    @Test
    void of_highPrecisionPrices() {
        Player player = Player.builder().id(7L).name("Lewandowski").build();
        Portfolio portfolio = Portfolio.builder()
                .player(player)
                .tokenQty(100)
                .avgBuyPrice(new BigDecimal("69.12345678"))
                .build();

        PortfolioDTO dto = PortfolioDTO.of(portfolio, new BigDecimal("75.98765432"));

        assertEquals(new BigDecimal("7598.76543200"), dto.getCurrentValue());
        assertEquals(new BigDecimal("686.41975400"), dto.getProfitLoss());
    }
}
