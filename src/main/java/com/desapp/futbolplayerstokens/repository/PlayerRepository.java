package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {
	List<Player> findByNameIgnoreCaseAndTeamIgnoreCase(String name, String team);
	List<Player> findByTeamIgnoreCase(String team);
}
