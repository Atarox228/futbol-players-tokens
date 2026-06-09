package com.desapp.futbolplayerstokens.modelo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioTest {

    @Test
    void builder() {
        User u = User.builder().id(1L).username("u").email("e").password("p")
                .role(User.Role.USER).balance(BigDecimal.ZERO).build();
        Player p = Player.builder().name("Messi").team("Inter Miami").build();
        Portfolio port = Portfolio.builder()
                .id(1L).user(u).player(p).tokenQty(50)
                .avgBuyPrice(new BigDecimal("80"))
                .build();
        assertEquals(1L, port.getId());
        assertSame(u, port.getUser());
        assertSame(p, port.getPlayer());
        assertEquals(50, port.getTokenQty());
        assertEquals(new BigDecimal("80"), port.getAvgBuyPrice());
    }

    @Test
    void noArgsConstructor() {
        Portfolio port = new Portfolio();
        assertNull(port.getId());
        assertNull(port.getUser());
        assertNull(port.getPlayer());
        assertEquals(0, port.getTokenQty());
        assertNull(port.getAvgBuyPrice());
    }

    @Test
    void allArgsConstructor() {
        User u = User.builder().id(2L).username("v").email("v@v.com").password("p")
                .role(User.Role.USER).balance(BigDecimal.TEN).build();
        Player p = Player.builder().name("Ronaldo").team("Al Nassr").build();
        Portfolio port = new Portfolio(10L, u, p, 30, new BigDecimal("100"));
        assertEquals(10L, port.getId());
        assertSame(u, port.getUser());
        assertSame(p, port.getPlayer());
        assertEquals(30, port.getTokenQty());
        assertEquals(new BigDecimal("100"), port.getAvgBuyPrice());
    }

    @Test
    void setters() {
        Portfolio port = new Portfolio();
        port.setTokenQty(15);
        port.setAvgBuyPrice(new BigDecimal("75.50"));
        assertEquals(15, port.getTokenQty());
        assertEquals(new BigDecimal("75.50"), port.getAvgBuyPrice());
    }
}
