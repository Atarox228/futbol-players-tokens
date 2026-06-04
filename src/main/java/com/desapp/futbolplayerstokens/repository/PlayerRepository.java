package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PlayerRepository extends JpaRepository<Player, Long> {
	List<Player> findByTeamIgnoreCase(String team);

    @Query("SELECT p FROM Player p WHERE " +
           "(:league IS NULL OR p.league = :league) AND " +
           "(:team IS NULL OR p.team = :team) AND " +
           "(:position IS NULL OR p.position = :position)")
    List<Player> findByFilters(
            @Param("league") String league,
            @Param("team") String team,
            @Param("position") String position
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Player p SET p.score = :score WHERE p.id = :id")
    int updateScoreById(@Param("id") Long id, @Param("score") BigDecimal score);

    List<Player> findByNameIgnoreCaseAndTeamIgnoreCase(String trim, String trim1);

    long countByScoreIsNotNull();

    @Query("SELECT p FROM Player p WHERE p.score IS NOT NULL ORDER BY p.score DESC")
    List<Player> findByScoreNotNullOrdered(Pageable pageable);

    @Query("SELECT p FROM Player p WHERE p.score IS NULL ORDER BY p.id ASC")
    List<Player> findByScoreNullOrdered(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM Player WHERE id NOT IN (SELECT MIN(p2.id) FROM Player p2 GROUP BY p2.name, p2.team)", nativeQuery = true)
    int deleteDuplicatesByNameAndTeam();
}
