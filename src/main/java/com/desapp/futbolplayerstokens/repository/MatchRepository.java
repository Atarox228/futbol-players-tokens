package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByTeam1IdOrTeam2Id(Long team1Id, Long team2Id);
}
