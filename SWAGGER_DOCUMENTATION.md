# Swagger API Documentation

## Acceso a Swagger UI

Una vez que la aplicación esté corriendo, puedes acceder a la documentación interactiva de Swagger en las siguientes URLs:

### URLs principales:
- **Swagger UI (Interfaz interactiva)**: http://localhost:8080/swagger-ui.html
- **JSON OpenAPI (Especificación)**: http://localhost:8080/v3/api-docs
- **YAML OpenAPI (Especificación)**: http://localhost:8080/v3/api-docs.yaml

## Endpoints Documentados

### 1. Authentication (`/auth`)
- `POST /auth/register` - Registrar nuevo usuario
- `POST /auth/login` - Iniciar sesión y obtener JWT token
- `POST /auth/logout` - Cerrar sesión

**Esquema de seguridad**: JWT Bearer Token (24 horas de validez)

### 2. Players (`/players`)
- `GET /players` - Obtener jugadores con filtros opcionales (liga, equipo, posición)
- `GET /players/hello` - Health check
- `GET /players/ranking` - Obtener ranking paginado de jugadores
- `GET /players/{id}` - Obtener jugador por ID con detalles completos
- `GET /players/{id}/quotes` - Obtener historial de cotizaciones de un jugador
- `POST /players/scrape` - Raspar y actualizar datos de jugadores de fuentes externas

### 3. Matches (`/api/matches`)
- `GET /api/matches/all` - Obtener todos los partidos
- `GET /api/matches/{id}` - Obtener partido por ID
- `GET /api/matches/team/{teamId}` - Obtener partidos de un equipo
- `POST /api/matches` - Crear nuevo partido
- `PUT /api/matches/{id}` - Actualizar partido existente
- `DELETE /api/matches/{id}` - Eliminar partido
- `POST /api/matches/scrape/today` - Raspar partidos del día actual

### 4. Scheduler (`/api/scheduler`)
- `GET /api/scheduler/status` - Obtener estado del programador
- `GET /api/scheduler/matches` - Obtener partidos programados
- `POST /api/scheduler/test/schedule-all` - Programar todos los partidos (Testing)

## Autenticación

Para usar los endpoints protegidos:

1. Usa `POST /auth/login` con credenciales válidas
2. Obtendrás un token JWT en la respuesta
3. En Swagger UI, haz clic en el botón "Authorize" arriba a la derecha
4. Pega el token en el formato: `Bearer <tu_token_aquí>`
5. Todos los siguientes requests incluirán el token automáticamente

## DTOs Documentados

### PlayerDTO
- `id`: ID único del jugador
- `name`: Nombre del jugador
- `team`: Equipo
- `league`: Liga (LALIGA, PREMIER_LEAGUE, etc.)
- `position`: Posición (Forward, Midfielder, etc.)
- `score`: Puntuación

### PlayerDetailDTO
- Incluye todos los campos de PlayerDTO más estadísticas detalladas:
  - Apariciones, minutos, goles, asistencias
  - Tiros a puerta, duelos aéreos, entradas
  - Tarjetas amarillas/rojas, entre otros

### MatchDTO
- `id`: ID único del partido
- `footballDataMatchId`: ID en la API de Football-Data
- `team1Id`: ID primer equipo
- `team2Id`: ID segundo equipo
- `matchTime`: Fecha y hora del partido

### QuoteDTO
- `id`: ID de la cotización
- `playerId`: ID del jugador
- `price`: Precio de la cotización
- `timestamp`: Fecha y hora
- `trigger`: Disparador (MANUAL, AUTOMATIC, etc.)

### PlayerRankingDTO
- `playerId`: ID del jugador
- `rank`: Posición en el ranking
- `score`: Puntuación

## Características de Swagger

✓ **Documentación interactiva**: Prueba los endpoints directamente desde el navegador
✓ **Ejemplos de request/response**: Ver estructura esperada de datos
✓ **Esquemas**: Definiciones claras de DTOs
✓ **Autenticación JWT**: Soporte para bearers tokens
✓ **Validación de parámetros**: Información sobre tipos de datos y restricciones
✓ **Códigos de respuesta HTTP**: Documentados para cada endpoint

## Configuración

La configuración de Swagger se encuentra en:
- **Clase OpenAPI Config**: `src/main/java/com/desapp/futbolplayerstokens/config/OpenApiConfig.java`
- **Propiedades**: `src/main/resources/application.yml` (sección `springdoc`)

## Mejoras Futuras Sugeridas

- Agregar ejemplos de request/response más completos
- Documentar códigos de error específicos
- Agregar más descripciones de negocio en los DTOs
- Configurar CORS para consumir desde frontend
