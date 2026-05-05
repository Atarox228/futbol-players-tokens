# AGENTS.md

## Propósito de este archivo
Guía para el agente que asiste en el desarrollo. El programador revisa y ajusta
todo código generado antes de integrarlo. El agente no debe hacer cambios
destructivos sin indicación explícita.

## Contexto rapido
- Proyecto Spring Boot 4 + Java 21 con JWT para autenticacion y endpoint protegido de jugadores.
- Entrada de app: `src/main/java/com/desapp/futbolplayerstokens/FutbolPlayersTokensApplication.java`.
- Persistencia con Spring Data JPA sobre H2 en memoria (`src/main/resources/application.yml`).

## Arquitectura y flujo principal
- Estructura por capas: `controller` -> `service`/`service.impl` -> `repository` -> `modelo`.
- Flujo login JWT:
  - `POST /auth/login` en `AuthController` autentica con `AuthenticationManager`.
  - `JwtUtil.generateToken(username)` crea token HS256 (expira en 24h).
  - `SecurityConfig` permite `/auth/**` y exige auth para el resto.
  - `JwtAuthenticationFilter` lee `Authorization: Bearer ...`, valida token y setea `SecurityContext`.
- Flujo registro:
  - `POST /auth/register` llama `UserService.registerUser`.
  - Se valida username unico (`UserRepository.findByUsername`) y se persiste password con BCrypt (`PasswordConfig`).
- Flujo player protegido:
  - `GET /players/{id}` en `PlayerControllerREST` usa `PlayerServiceImpl` y devuelve `PlayerDTO`.

## Convenciones del codigo (especificas de este repo)
- DTOs de salida con mapper estatico en el propio DTO (`PlayerDTO.toDTO(Player)`).
- Errores de dominio se manejan con `RuntimeException` y se traducen en controller con `try/catch` a `400` o `404`.
- `User.Role` se mapea a Spring Security con `.roles(user.getRole().name())` en `CustomUserDetailsService`.
- Entidades usan Lombok (`@Builder`, `@Getter/@Setter`) y JPA annotations basicas.
- Requests de auth estan como clases internas estaticas en `AuthController` (`RegisterRequest`, `LoginRequest`).
- **Manejo de errores**: lanzar excepciones de negocio con mensajes descriptivos; no silenciar excepciones con bloques catch vacíos.
- **No usar `findAll()` + filtro en memoria** para consultas con filtros; usar métodos derivados de JPA o `@Query`.
- **Siempre generar interface + impl** para servicios nuevos. No poner lógica de negocio en controllers o repositorios.
- **No hardcodear valores sensibles** (como claves secretas) en el código; usar properties o variables de entorno.


## Workflows de desarrollo
- Build y tests (verificado):
  - `./gradlew test` (Windows: `gradlew.bat test`) -> OK al 2026-05-04.
  - `./gradlew clean build` genera jar en `build/libs/`.
- Run local:
  - `./gradlew bootRun` o `java -jar build/libs/futbol-players-tokens-0.0.1-SNAPSHOT.jar`.
- DB local:
  - H2 in-memory `jdbc:h2:mem:testdb`, `ddl-auto: update`, SQL visible (`show-sql: true`).

## Integraciones y puntos sensibles
- Dependencia externa principal de seguridad: `io.jsonwebtoken:jjwt-*`.
- `JwtUtil` usa `SECRET_KEY` hardcodeada; no mover a produccion sin externalizar secreto.
- No hay `@ControllerAdvice` global: si agregas endpoints, replica el patron actual de manejo de errores o introduce uno consistente en todo el proyecto.
- El repositorio de tests en `src/test` actualmente solo tiene `contextLoads`; cualquier cambio funcional deberia agregar tests por capa siguiendo nombres tipo `*Controller*Test` / `*Service*Test`.

## Estructura de paquetes esperada
controller/          ← REST controllers + DTOs en controller/dto/
service/             ← interfaces de servicio
service/impl/        ← implementaciones
repository/          ← Spring Data JPA repositories
modelo/              ← entidades JPA
config/              ← configuración de beans y properties
scheduler/           ← jobs automáticos (@Scheduled)

Cuando el agente cree nuevas clases, debe respetar este esquema. Si una
funcionalidad nueva necesita un subpaquete, debe consultarlo antes de crearlo.