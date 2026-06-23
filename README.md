# Futbol Players Tokens

Proyecto Spring Boot con JWT para validar logins de usuarios.

## Dependencias clave

- Spring Boot 4.0.5
- Spring Web MVC
- Spring Data JPA
- Spring Security
- JJWT 0.11.5
- H2 en memoria para pruebas locales

## Cómo ejecutar

1. Compilar el proyecto:

```bash
./gradlew clean build
```

2. Ejecutar la aplicación:

```bash
java -jar build/libs/futbol-players-tokens-0.0.1-SNAPSHOT.jar --spring.devtools.restart.enabled=false
```

O con Gradle:

```bash
./gradlew bootRun
```

La aplicación se levanta en `http://localhost:8080`.

## Documentación Swagger

- Swagger UI interactiva: `http://localhost:8080/swagger-ui.html`
- API docs JSON: `http://localhost:8080/v3/api-docs`
- API docs YAML: `http://localhost:8080/v3/api-docs.yaml`

Para usar los endpoints protegidos, haga clic en el botón "Authorize" en Swagger UI y pegue el token en el formato:

```text
Bearer <jwt-token>
```

## Endpoints JWT

### 1. Registro de usuario

- URL: `POST /auth/register`
- Body JSON:
  ```json
  {
    "username": "miusuario",
    "password": "miclave",
    "email": "miemail@example.com"
  }
  ```
- Respuesta exitosa: `User registered successfully`

### 2. Login y obtención de token

- URL: `POST /auth/login`
- Body JSON:
  ```json
  {
    "username": "miusuario",
    "password": "miclave"
  }
  ```
- Respuesta exitosa:
  ```json
  {
    "token": "<jwt-token>"
  }
  ```

### 3. Acceder a rutas protegidas

- Agrega el header HTTP:
  ```text
  Authorization: Bearer <jwt-token>
  ```
- Ejemplo protegido:
  ```bash
  curl -X GET http://localhost:8080/players/1 \
    -H "Authorization: Bearer <jwt-token>"
  ```

## Comportamiento verificado

- Registro de usuario válido ✅
- Login válido y generación de JWT válido ✅
- Acceso a endpoint protegido con JWT válido ✅
- Acceso a endpoint protegido sin token: `403 Forbidden` ✅
- Endpoint protegido con token inválido: `403 Forbidden` ✅

## Sistema de Valuación de Jugadores

El sistema permite valuar jugadores usando estrategias configurables con dos modos de puntuación:

### Modos de Puntuación

- **GENERAL**: Usa las mismas métricas y pesos para todos los jugadores, independientemente de su posición.
- **POSITION**: Usa fórmulas diferenciadas según la posición del jugador (FW, MF, DF, GK).

### Estrategias de Valuación

Cada tipo de estrategia (`GENERAL`, `FORWARD`, `MIDFIELDER`, `DEFENDER`, `GOALKEEPER`) tiene:
- `valorBase`: Precio base del jugador.
- `factorEscala`: Multiplicador del score calculado.
- `weights`: Pesos configurables para cada métrica (goles, asistencias, rating, etc.).
- `version`: Historial de versiones. Cada actualización crea una nueva versión.

### Endpoints de Estrategias

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/strategies/active` | Obtiene la estrategia GENERAL activa (última versión) |
| `GET` | `/api/strategies` | Lista todas las estrategias activas (una por tipo) |
| `GET` | `/api/strategies/mode` | Obtiene el modo de puntuación actual (GENERAL o POSITION) |
| `PUT` | `/api/strategies/mode` | Cambia el modo de puntuación. Recalcula automáticamente todas las valuaciones |
| `GET` | `/api/strategies/{type}` | Obtiene la estrategia activa de un tipo específico |
| `GET` | `/api/strategies/{type}/history` | Historial de versiones de una estrategia |
| `PUT` | `/api/strategies/{type}` | Actualiza una estrategia (nueva versión). Los pesos deben sumar ≤ 1. **Recalcula automáticamente todas las valuaciones** |
| `PUT` | `/api/strategies/{type}/normalized` | Actualiza una estrategia normalizando pesos para que sumen exactamente 1. **Recalcula automáticamente todas las valuaciones** |

### Configuración Global de Valuación

La configuración de valuación es **global**: cualquier cambio en las estrategias o en el modo de puntuación dispara automáticamente el recálculo de **todas** las valuaciones de todos los jugadores. Esto asegura que:
- Si se cambia el modo GENERAL ↔ POSITION, se recalculan todos los jugadores.
- Si se actualizan los pesos de cualquier estrategia, se recalculan todos los jugadores afectados.
- Si se cambia `valorBase` o `factorEscala`, se recalculan todos los jugadores.

### Ejemplos de uso

#### Cambiar modo a POSITION:
```bash
curl -X PUT http://localhost:8080/api/strategies/mode \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"mode": "POSITION"}'
```

#### Actualizar estrategia GENERAL con pesos personalizados:
```bash
curl -X PUT http://localhost:8080/api/strategies/GENERAL \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "valorBase": 100.00,
    "factorEscala": 50.00,
    "weights": {
      "goals": 0.30,
      "assists": 0.20,
      "rating": 0.25,
      "minutes": 0.15,
      "tackles": 0.10
    }
  }'
```

#### Actualizar con normalización automática de pesos:
```bash
curl -X PUT http://localhost:8080/api/strategies/FORWARD/normalized \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "valorBase": 100.00,
    "factorEscala": 50.00,
    "weights": {
      "goals": 0.5,
      "assists": 0.3,
      "dribbles": 0.2
    }
  }'
```

### Recálculo Manual de Valuaciones

```bash
curl -X POST http://localhost:8080/quotes/recalculate \
  -H "Authorization: Bearer <token>"
```

### Fórmula de Valuación

El precio de un jugador se calcula como:

```
score = Σ(weight_positive × normalized_positive) - Σ(weight_negative × normalized_negative)
score = clamp(score, 0, 1)
price = valorBase + score × factorEscala
```

Cada métrica se normaliza dividiendo por un valor máximo (ej: goles/30, asistencias/20, rating normalizado a [0,1]).

### Ranking de Jugadores

```bash
curl -X GET "http://localhost:8080/players/ranking?page=0&size=20" \
  -H "Authorization: Bearer <token>"
```

### Estrategias por Posición

Cada posición usa métricas específicas:

- **FW (Delantero)**: goles, tiros al arco, regates, asistencias, pases clave, tarjetas.
- **MF (Mediocampista)**: pases clave, precisión de pases, asistencias, regates, tackles, rating, tarjetas.
- **DF (Defensor)**: tackles, intercepciones, despejes, bloqueos, rating, tarjetas, faltas, goles en contra.
- **GK (Arquero)**: despejes, bloqueos, intercepciones, rating, tarjetas rojas.

Si un jugador tiene una posición desconocida o nula, se usa la estrategia GENERAL como fallback.

## Notas importantes

- El JWT se genera con una clave secreta fija en `JwtUtil`.
- En producción, reemplaza `SECRET_KEY` por una clave segura y administra la configuración con variables de entorno.
- El proyecto usa H2 en memoria para pruebas locales, por lo que los datos se pierden al reiniciar la aplicación.
