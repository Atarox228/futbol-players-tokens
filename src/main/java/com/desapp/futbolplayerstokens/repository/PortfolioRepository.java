package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByUserId(Long userId);
    Optional<Portfolio> findByUserIdAndPlayerId(Long userId, Long playerId);
}

