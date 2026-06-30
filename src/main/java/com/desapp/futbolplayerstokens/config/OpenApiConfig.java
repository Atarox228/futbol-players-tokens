package com.desapp.futbolplayerstokens.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Futbol Players Tokens API",
        version = "1.0.0",
        description = "API REST para gestión de jugadores de fútbol, cotizaciones, partidos y configuración de estrategias de valuación. Soporta dos modos de puntuación: GENERAL (mismas métricas para todos) y POSITION (métricas según la posición del jugador). Los pesos de las valuaciones pueden configurarse y normalizarse.",
        contact = @Contact(
            name = "DesApp Team",
            url = "https://github.com/desapp"
        )
    ),
    servers = {}
)
@SecurityScheme(
    name = "bearer-jwt",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT token. Use token obtained from /auth/login endpoint",
    in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
