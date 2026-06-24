package com.desapp.futbolplayerstokens;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class FutbolPlayersTokensApplication {

    public static void main(String[] args) {
        loadEnv();

        String chromeDriver = System.getProperty("CHROMEDRIVER_BIN");

        if (chromeDriver != null && !chromeDriver.isBlank()) {
            WebDriverManager.chromedriver().setup();
        }

        SpringApplication.run(FutbolPlayersTokensApplication.class, args);
    }

    private static void loadEnv() {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path dockerEnvPath = projectDir.resolve(".env.docker");
        Path localEnvPath = projectDir.resolve(".env");

        if (!loadEnvFile(dockerEnvPath)) {
            loadEnvFile(localEnvPath);
        }
    }

    private static boolean loadEnvFile(Path envPath) {
        if (!Files.exists(envPath)) {
            return false;
        }

        int loadedVariables = 0;

        try (BufferedReader reader = Files.newBufferedReader(envPath)) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                int separatorIndex = line.indexOf('=');

                if (line.isBlank()
                        || line.startsWith("#")
                        || separatorIndex <= 0) {
                    continue;
                }

                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 1).trim();

                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }

                if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }

                System.setProperty(key, value);
                loadedVariables++;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo archivo .env: " + envPath.toAbsolutePath(), e);
        }

        return loadedVariables > 0;
    }   
}