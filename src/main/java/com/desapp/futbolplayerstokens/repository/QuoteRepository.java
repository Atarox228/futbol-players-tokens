package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    @Query("SELECT q FROM Quote q WHERE q.player.id = :playerId ORDER BY q.timestamp DESC")
    List<Quote> findByPlayerIdOrderByTimestampDesc(@Param("playerId") Long playerId);

    Optional<Quote> findTopByPlayerIdOrderByTimestampDesc(Long playerId);
}


