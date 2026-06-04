package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.User;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    private Portfolio p1;
    private Portfolio p2;
    private User user1;
    private Player player1;

    @BeforeEach
    void setUp() {
        portfolioRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .username("portfolio-user-1")
                .email("portfolio-user-1@example.com")
                .password("password")
                .role(User.Role.USER)
                .balance(new BigDecimal("1000"))
                .build());
        User user2 = userRepository.save(User.builder()
                .username("portfolio-user-2")
                .email("portfolio-user-2@example.com")
                .password("password")
                .role(User.Role.USER)
                .balance(new BigDecimal("1000"))
                .build());

        player1 = playerRepository.save(Player.builder()
                .name("Portfolio Player 1")
                .team("Team A")
                .league("League A")
                .position("Forward")
                .build());
        Player player2 = playerRepository.save(Player.builder()
                .name("Portfolio Player 2")
                .team("Team B")
                .league("League B")
                .position("Midfielder")
                .build());

        p1 = Portfolio.builder()
                .user(user1)
                .player(player1)
                .tokenQty(50)
                .avgBuyPrice(new BigDecimal("1.0"))
                .build();

        p2 = Portfolio.builder()
                .user(user2)
                .player(player2)
                .tokenQty(20)
                .avgBuyPrice(new BigDecimal("2.0"))
                .build();
    }

    @Test
    void testFindByUser() {
        portfolioRepository.save(p1);
        portfolioRepository.save(p2);

        List<Portfolio> list = portfolioRepository.findByUser(user1);
        assertEquals(1, list.size());
        assertEquals(player1.getId(), list.getFirst().getPlayer().getId());
    }

    @Test
    void testFindByUserAndPlayer() {
        portfolioRepository.save(p1);
        Optional<Portfolio> opt = portfolioRepository.findByUserAndPlayer(user1, player1);
        assertTrue(opt.isPresent());
        assertEquals(50, opt.get().getTokenQty());
    }
}
