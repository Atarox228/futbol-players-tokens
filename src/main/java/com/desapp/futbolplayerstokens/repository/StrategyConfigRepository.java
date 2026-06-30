package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.StrategyConfig;
import com.desapp.futbolplayerstokens.modelo.StrategyConfig.StrategyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StrategyConfigRepository extends JpaRepository<StrategyConfig, Long> {
    Optional<StrategyConfig> findTopByTypeOrderByVersionDesc(StrategyType type);

    List<StrategyConfig> findByTypeOrderByVersionDesc(StrategyType type);

    @Query("SELECT s FROM StrategyConfig s WHERE s.version = (SELECT MAX(s2.version) FROM StrategyConfig s2 WHERE s2.type = s.type)")
    List<StrategyConfig> findAllActive();

    Optional<StrategyConfig> findTopByOrderByVersionDesc();

    Optional<StrategyConfig> findFirstByTypeAndVersionLessThanOrderByVersionDesc(StrategyType type, Integer version);
}

