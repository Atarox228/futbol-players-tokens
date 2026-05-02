package com.desapp.futbolplayerstokens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.github.bonigarcia.wdm.WebDriverManager;

@SpringBootApplication
@EnableScheduling
public class FutbolPlayersTokensApplication {

    public static void main(String[] args) {
        // Configure WebDriverManager for Docker environments
        String chromeDriver = System.getenv("CHROMEDRIVER_BIN");
        String chromeBin = System.getenv("CHROMIUM_BIN");

        if (chromeDriver != null && !chromeDriver.isBlank()) {
            WebDriverManager.chromedriver().driverVersion("LATEST").setup();
        }

        SpringApplication.run(FutbolPlayersTokensApplication.class, args);
    }

}
