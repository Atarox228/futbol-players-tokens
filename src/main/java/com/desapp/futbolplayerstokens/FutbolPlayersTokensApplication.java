package com.desapp.futbolplayerstokens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FutbolPlayersTokensApplication {

    public static void main(String[] args) {
        SpringApplication.run(FutbolPlayersTokensApplication.class, args);
    }

}
