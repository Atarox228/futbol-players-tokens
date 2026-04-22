package com.desapp.futbolplayerstokens.config;

import com.desapp.futbolplayerstokens.repository.PlayerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final PlayerRepository playerRepository;

    public DatabaseInitializer(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        long count = playerRepository.count();
        if (count > 0) {
            System.out.println("\n🗑️  Limpiando BD... Eliminando " + count + " jugadores anteriores");
            playerRepository.deleteAll();
            System.out.println("✓ BD limpiada exitosamente\n");
        }
    }
}
