package com.desapp.futbolplayerstokens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableScheduling
public class FutbolPlayersTokensApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        String chromeDriver = System.getenv("CHROMEDRIVER_BIN");
        String chromeBin = System.getenv("CHROMIUM_BIN");

        if (chromeDriver != null && !chromeDriver.isBlank()) {
            WebDriverManager.chromedriver().driverVersion("LATEST").setup();
        }

        SpringApplication.run(FutbolPlayersTokensApplication.class, args);
    }

}
