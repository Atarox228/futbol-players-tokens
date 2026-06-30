package com.desapp.futbolplayerstokens;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        Properties dbProps = parseDatabaseUrl();

        if (dbProps != null) {
            StandardEnvironment env = new StandardEnvironment();
            env.getPropertySources().addFirst(new PropertiesPropertySource("databaseUrl", dbProps));
            SpringApplication app = new SpringApplication(FutbolPlayersTokensApplication.class);
            app.setEnvironment(env);
            app.run(args);
        } else {
            SpringApplication.run(FutbolPlayersTokensApplication.class, args);
        }
    }

    private static void loadEnv() {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path dockerEnvPath = projectDir.resolve(".env.docker");
        Path localEnvPath = projectDir.resolve(".env");

        if (!loadEnvFile(dockerEnvPath)) {
            loadEnvFile(localEnvPath);
        }
    }

    private static Properties parseDatabaseUrl() {
        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl == null || databaseUrl.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile("postgresql://([^:]+):([^@]+)@([^:]+)(?::(\\d+))?/(\\w+)");
        Matcher matcher = pattern.matcher(databaseUrl);

        if (matcher.matches()) {
            String user = matcher.group(1);
            String password = matcher.group(2);
            String host = matcher.group(3);
            String port = matcher.group(4) != null ? matcher.group(4) : "5432";
            String db = matcher.group(5);

            Properties props = new Properties();
            props.setProperty("spring.datasource.url", "jdbc:postgresql://" + host + ":" + port + "/" + db);
            props.setProperty("spring.datasource.username", user);
            props.setProperty("spring.datasource.password", password);
            return props;
        }

        return null;
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