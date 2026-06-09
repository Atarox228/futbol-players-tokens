package com.desapp.futbolplayerstokens.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Futbol Players Tokens API",
        version = "1.0.0",
        description = "API REST para gestión de jugadores de fútbol, cotizaciones y partidos.",
        contact = @Contact(
            name = "DesApp Team",
            url = "https://github.com/desapp"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",
            description = "Local Server"
        ),
        @Server(
            url = "http://localhost:8081",
            description = "Alternative Local Server"
        )
    },
    security = @SecurityRequirement(name = "bearer-jwt")
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
