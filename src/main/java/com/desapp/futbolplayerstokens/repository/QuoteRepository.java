package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    @Query("SELECT q FROM Quote q WHERE q.player.id = :playerId ORDER BY q.timestamp DESC")
    List<Quote> findByPlayerIdOrderByTimestampDesc(@Param("playerId") Long playerId);

    Optional<Quote> findTopByPlayerIdOrderByTimestampDesc(Long playerId);

    @Query("SELECT q.price FROM Quote q WHERE q.player.id = :playerId AND q.timestamp >= :since ORDER BY q.timestamp DESC")
    List<BigDecimal> findPricesByPlayerIdSince(@Param("playerId") Long playerId, @Param("since") java.time.LocalDateTime since);

    @Query("SELECT q FROM Quote q WHERE q.player.id = :playerId AND q.timestamp BETWEEN :start AND :end AND q.strategyVersion = :version ORDER BY q.timestamp DESC")
    List<Quote> findByPlayerIdAndTimestampBetweenAndStrategyVersion(
            @Param("playerId") Long playerId,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end,
            @Param("version") Integer strategyVersion);

    @Query("SELECT q FROM Quote q WHERE q.strategyId = :strategyId AND q.strategyVersion = :version AND q.timestamp BETWEEN :start AND :end")
    List<Quote> findByStrategyAndVersionAndTimestampBetween(
            @Param("strategyId") Long strategyId,
            @Param("version") Integer version,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end);
}


