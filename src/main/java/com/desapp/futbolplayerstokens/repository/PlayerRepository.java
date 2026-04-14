package com.desapp.futbolplayerstokens.repository;

import com.desapp.futbolplayerstokens.modelo.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
}
