package com.desapp.futbolplayerstokens.config;

import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Inicializa variables de entorno desde el archivo .env
 * al startup de la aplicación Spring Boot
 */
@Configuration
public class DotEnvInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DotEnvInitializer.class);

    public DotEnvInitializer() {
        loadEnvFile();
    }

    /**
     * Carga el archivo .env y setea las variables en System.setProperty
     */
    private void loadEnvFile() {
        // Buscar en raíz del proyecto
        String userDir = System.getProperty("user.dir");
        Path envPath = Paths.get(userDir, ".env");
        
        logger.info("🔍 Buscando .env en: {}", envPath.toAbsolutePath());
        
        if (!Files.exists(envPath)) {
            logger.warn("⚠️ Archivo .env no encontrado en: {}", envPath.toAbsolutePath());
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
            String line;
            int loadedVars = 0;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Ignorar comentarios y líneas vacías
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parsear KEY=VALUE
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts.length > 1 ? parts[1].trim() : "";
                    
                    // Remover comillas si existen
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }

                    System.setProperty(key, value);
                    logger.debug("✓ Cargada variable: {} = {}", key, maskSensitiveValue(key, value));
                    loadedVars++;
                }
            }
            
            logger.info("✅ Archivo .env cargado correctamente. {} variables cargadas", loadedVars);
        } catch (IOException e) {
            logger.error("❌ Error al leer archivo .env: {}", e.getMessage());
        }
    }
    
    /**
     * Enmascara valores sensibles en los logs
     */
    private String maskSensitiveValue(String key, String value) {
        if (key.contains("TOKEN") || key.contains("PASSWORD") || key.contains("SECRET")) {
            return value.substring(0, Math.min(3, value.length())) + "***";
        }
        return value;
    }
}
