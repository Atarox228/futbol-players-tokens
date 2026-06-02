package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Portfolio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PortfolioRepositoryTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    private Portfolio p1;
    private Portfolio p2;

    @BeforeEach
    void setUp() {
        portfolioRepository.deleteAll();

        p1 = Portfolio.builder()
                .userId(1L)
                .playerId(10L)
                .tokenQty(50)
                .avgBuyPrice(new BigDecimal("1.0"))
                .currentValue(new BigDecimal("50.0"))
                .profitLoss(new BigDecimal("0.0"))
                .build();

        p2 = Portfolio.builder()
                .userId(2L)
                .playerId(11L)
                .tokenQty(20)
                .avgBuyPrice(new BigDecimal("2.0"))
                .currentValue(new BigDecimal("40.0"))
                .profitLoss(new BigDecimal("0.0"))
                .build();
    }

    @Test
    void testFindByUserId() {
        portfolioRepository.save(p1);
        portfolioRepository.save(p2);

        List<Portfolio> list = portfolioRepository.findByUserId(1L);
        assertEquals(1, list.size());
        assertEquals(10L, list.get(0).getPlayerId());
    }

    @Test
    void testFindByUserIdAndPlayerId() {
        portfolioRepository.save(p1);
        Optional<Portfolio> opt = portfolioRepository.findByUserIdAndPlayerId(1L, 10L);
        assertTrue(opt.isPresent());
        assertEquals(50, opt.get().getTokenQty());
    }
}

