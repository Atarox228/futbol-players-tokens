package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    @Query("SELECT p FROM Player p WHERE " +
           "(:league IS NULL OR p.league = :league) AND " +
           "(:team IS NULL OR p.team = :team) AND " +
           "(:position IS NULL OR p.position = :position)")
    List<Player> findByFilters(
            @Param("league") String league,
            @Param("team") String team,
            @Param("position") String position
    );
}
