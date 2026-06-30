package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Player;
import com.desapp.futbolplayerstokens.modelo.Portfolio;
import com.desapp.futbolplayerstokens.modelo.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByUser(User user);
    Page<Portfolio> findByUser(User user, Pageable pageable);
    Optional<Portfolio> findByUserAndPlayer(User user, Player player);

    @Query("SELECT COUNT(DISTINCT p.user.id) FROM Portfolio p")
    long countDistinctUserId();
}

