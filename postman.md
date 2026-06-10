# Postman Collection — Futbol Players Tokens

## Setup

- **Base URL:** `http://localhost:8080`
- **Auth:** Los endpoints marcados como 🔐 requieren token JWT en header `Authorization: Bearer <token>`.
  Obtener token: `POST /auth/login` → copiar `token` del response.
- **Content-Type:** `application/json`

---

## Autenticación (público)

### POST /auth/register
Registrar nuevo usuario.

**Body:**
```json
{
  "username": "juan_perez",
  "password": "password123",
  "email": "juan@example.com"
}
```

**Response 200:**
```
User registered successfully
```

**Response 400:**
```
El usuario ya existe
```

---

### POST /auth/login
Iniciar sesión. Devuelve JWT y también setea cookie `authToken`.

**Body:**
```json
{
  "username": "juan_perez",
  "password": "password123"
}
```

**Response 200:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Login successful",
  "userId": 1
}
```

**Response 400:**
```
Invalid credentials
```

---

### POST /auth/logout
Invalida la cookie `authToken`.

**Response 200:**
```json
{
  "message": "Logout successful"
}
```

---

## Players — 🔐 requieren auth (excepto /scrape)

### GET /players/hello
Health check.

**Response 200:**
```
Hello World
```

---

### GET /players
Listado paginado de jugadores con filtros.

**Query params:** `league`, `team`, `position`, `page` (default 0), `size` (default 20), `sort` (default `id`)

**Ejemplos:**
```
GET /players
GET /players?league=LaLiga&page=0&size=20
GET /players?team=Barcelona
GET /players?position=Forward&page=1&size=10&sort=score,desc
```

**Response 200:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Cristiano Ronaldo",
      "team": "Al Nassr",
      "league": "Saudi League",
      "position": "Forward",
      "score": 85.50,
      "altPosition": "Midfielder"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20,
  "sort": { "sorted": true, "unsorted": false, "empty": false },
  "first": true,
  "last": false,
  "empty": false
}
```

---

### GET /players/ranking
Ranking paginado por puntuación.

**Query params:** `page` (default 0), `size` (default 20)

**Response 200:**
```json
[
  {
    "playerId": "1",
    "playerName": "Lionel Messi",
    "score": 95.20,
    "position": "Forward",
    "team": "Inter Miami",
    "league": "MLS",
    "rank": 1
  },
  {
    "playerId": "2",
    "playerName": "Kylian Mbappé",
    "score": 92.80,
    "position": "Forward",
    "team": "Real Madrid",
    "league": "LaLiga",
    "rank": 2
  }
]
```

---

### GET /players/{id}
Detalle completo de un jugador.

**Response 200:**
```json
{
  "id": 1,
  "name": "Cristiano Ronaldo",
  "rating": 8.5,
  "team": "Al Nassr",
  "league": "Saudi League",
  "position": "Forward",
  "appearances": 35,
  "minutes": 2850,
  "goals": 18,
  "assists": 8,
  "shotsOnTarget": 45.5,
  "aerialWon": 250.0,
  "faults": 25.0,
  "offsidesGiven": 5.0,
  "clears": 120.0,
  "dribbled": 15.0,
  "tackles": 45.0,
  "interceptions": 60.0,
  "blocks": 30.0,
  "ownGoals": 0,
  "keyPasses": 25.0,
  "dribbles": 35.0,
  "faulted": 10.0,
  "offsides": 3.0,
  "dispossesed": 50.0,
  "turnover": 40.0,
  "passAccuracy": 85.5,
  "yellowCards": 3,
  "redCards": 0,
  "playerOfTheMatch": 2,
  "score": 85.50,
  "altPosition": "Midfielder",
  "availableTokens": 500,
  "totalTokens": 1000
}
```

**Response 404:**
(sin body)

---

### GET /players/{id}/quotes
Historial de cotizaciones de un jugador.

**Response 200:**
```json
[
  {
    "id": 1,
    "playerId": 5,
    "price": 1250.50,
    "timestamp": "2026-06-01T10:30:00",
    "strategyId": 1,
    "strategyVersion": 1,
    "trigger": "MANUAL"
  }
]
```

---

### POST /players/scrape
🟢 Público. Scrapeo forzado completo de todas las ligas (puede tardar minutos).

**Response 200:**
```
✓ Scraping forzado completado correctamente
⏱️ Tiempo total: 5 min 23 seg
```

**Response 500:**
```
❌ Error: mensaje de error
```

---

### POST /players/scrape/new-only
🟢 Público. Scrapea solo jugadores nuevos no existentes en BD.

**Response 200:**
```
✓ Se encontraron y guardaron 12 jugadores NUEVOS de todas las ligas
⏱️ Tiempo total: 2 min 10 seg
```

---

### POST /players/scrape/team/{id}
🟢 Público. Scrapea y sobreescribe jugadores de un equipo.

**Path params:**
| id | Team |
|----|------|
| 1 | Barcelona |
| 2 | Real Madrid |
| 3 | Atlético Madrid |
| 4 | Manchester City |
| 5 | Manchester United |
| 6 | Liverpool |
| (etc. según `TeamEnum`) |

**Response 200:**
```
✓ Scraping completado para Barcelona. Jugadores procesados. Tiempo: 0 min 45 seg
```

**Response 400:**
```
❌ ID de equipo inválida: 9999
```

---

### POST /players/scrape/league/{league}
🟢 Público. Scrapea todos los equipos de una liga.

**Path params:** `LaLiga`, `Premier League`, `Bundesliga`, `Serie A`, `Ligue 1`

**Response 200:**
```
✓ Scraping completo de LaLiga finalizado (todos los equipos). Tiempo: 3 min 12 seg
```

**Response 400:**
```
❌ Liga inválida. Usá: LaLiga, Premier League, Bundesliga, Serie A, Ligue 1
```

---

## Órdenes — 🔐 requieren auth

### POST /orders/buy
Crear orden de compra.

**Body:**
```json
{
  "playerId": 5,
  "quantity": 5,
  "idempotencyKey": "uuid-unico-que-no-se-repita",
  "maxPrice": 100.00
}
```

**Response 200:**
```json
{
  "id": 10,
  "userId": 1,
  "playerId": 5,
  "playerName": "Messi",
  "type": "BUY",
  "quantity": 5,
  "priceAtOrder": 95.50,
  "total": 477.50,
  "idempotencyKey": "uuid-unico-que-no-se-repita",
  "createdAt": "2026-06-09T12:00:00",
  "status": "FILLED",
  "remainingQuantity": 0
}
```

---

### POST /orders/sell
Crear orden de venta.

**Body:**
```json
{
  "playerId": 5,
  "quantity": 3,
  "idempotencyKey": "uuid-unico-para-venta",
  "minPrice": 1.00
}
```

**Response 200:** (mismo formato que buy, type="SELL")

---

### GET /orders/transactions
Transacciones del usuario autenticado (paginado).

**Query params:** `page` (default 0), `size` (default 20), `sort` (default `createdAt`)

**Response 200:**
```json
{
  "content": [
    {
      "id": 10,
      "userId": 1,
      "playerId": 5,
      "playerName": "Messi",
      "type": "BUY",
      "quantity": 5,
      "priceAtOrder": 95.50,
      "total": 477.50,
      "idempotencyKey": "key-123",
      "createdAt": "2026-06-09T10:30:00",
      "status": "FILLED",
      "remainingQuantity": 0
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

### GET /orders/book
Libro de órdenes abiertas global (paginado).

**Query params:** `type` (`BUY`/`SELL`, optional), `page`, `size`, `sort`

**Response 200:** Mismo formato que transactions.

---

### GET /orders/pending
Órdenes pendientes del usuario autenticado (paginado).

**Query params:** `type` (`BUY`/`SELL`, optional), `page`, `size`, `sort`

**Response 200:** Mismo formato que transactions.

---

### POST /orders/sell-all
Vende todos los tokens del usuario autenticado.

**Response 200:**
```json
[
  { "id": 11, "playerId": 1, "type": "SELL", "quantity": 10, ... },
  { "id": 12, "playerId": 5, "type": "SELL", "quantity": 5, ... }
]
```

---

### GET /orders/player/{playerId}
Órdenes asociadas a un jugador específico.

**Response 200:** `OrderDTO[]`

---

### POST /orders/{id}/cancel
Cancela una orden pendiente.

**Response 200:**
```json
{
  "id": 10,
  "status": "CANCELLED",
  ...
}
```

---

## Usuarios — 🔐 requieren auth

### GET /users/{id}/portfolio
Portfolio del usuario (paginado). El usuario autenticado debe coincidir con `{id}`.

**Query params:** `page` (default 0), `size` (default 20), `sort` (default `id`)

**Response 200:**
```json
{
  "content": [
    {
      "playerId": 1,
      "playerName": "Messi",
      "tokenQty": 10,
      "avgBuyPrice": 50.00,
      "currentPrice": 75.00,
      "currentValue": 750.00,
      "profitLoss": 250.00
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

---

### GET /users/{id}/transactions
Historial de transacciones del usuario.

**Response 200:** `OrderDTO[]`

---

### GET /users/{id}/balance
Saldo del usuario.

**Response 200:**
```json
{
  "userId": 1,
  "username": "juan_perez",
  "balance": 1000.00
}
```

---

## Cotizaciones — 🔐 requieren auth

### POST /quotes/recalculate
Recalcula cotizaciones de todos los jugadores manualmente.

**Response 202:**
```
Recalculation triggered for all players
```

---

### GET /quotes/player/{id}/current
Cotización actual de un jugador.

**Response 200:**
```json
{
  "id": 1,
  "playerId": 5,
  "price": 1250.50,
  "timestamp": "2026-06-09T12:00:00",
  "strategyId": 1,
  "strategyVersion": 1,
  "trigger": "MANUAL"
}
```

---

## Partidos — 🔐 requieren auth (excepto /scrape)

### GET /api/matches/all
Todos los partidos registrados.

**Response 200:**
```json
[
  {
    "id": 1,
    "footballDataMatchId": 12345,
    "team1Id": 10,
    "team2Id": 20,
    "matchTime": "2026-06-09T15:00:00"
  }
]
```

---

### GET /api/matches/{id}
Partido por ID.

**Response 200:** `MatchDTO`
**Response 404:** (sin body)

---

### GET /api/matches/team/{teamId}
Partidos de un equipo.

**Response 200:** `MatchDTO[]`

---

### POST /api/matches
Crear un nuevo partido.

**Body:**
```json
{
  "footballDataMatchId": 12345,
  "team1Id": 10,
  "team2Id": 20,
  "matchTime": "2026-06-09T15:00:00"
}
```

**Response 201:**
```json
{
  "id": 1,
  "footballDataMatchId": 12345,
  "team1Id": 10,
  "team2Id": 20,
  "matchTime": "2026-06-09T15:00:00"
}
```

---

### PUT /api/matches/{id}
Actualizar un partido.

**Body:** `MatchDTO` (mismos campos que POST)

**Response 200:** `MatchDTO`
**Response 404:** (sin body)

---

### DELETE /api/matches/{id}
Eliminar un partido.

**Response 204:** (sin contenido)

---

### POST /api/matches/scrape/today
🟢 Público. Scrapea partidos del día desde fuente externa.

**Response 200:** `MatchDTO[]`

---

## Scheduler — 🟢 público

### GET /api/scheduler/status
Estado del programador de partidos.

**Response 200:**
```json
{
  "scheduledMatches": 5,
  "status": "ACTIVE"
}
```

---

### GET /api/scheduler/matches
Partidos programados actualmente.

**Response 200:**
```json
{
  "total": 5,
  "matches": [ ... ]
}
```

---

### POST /api/scheduler/test/schedule-all
Programa todos los partidos para testing.

**Response 200:**
```json
{
  "success": true,
  "scheduledMatches": 5,
  "message": "5 partidos programados para testing"
}
```

---

## Estrategias — 🔐 requieren auth

### GET /api/strategies/active
Estrategia activa global (última versión GENERAL).

**Response 200:**
```json
{
  "type": "GENERAL",
  "version": 1,
  "valorBase": 10.0,
  "factorEscala": 1.0,
  "weights": {
    "goals": 0.35,
    "assists": 0.15,
    "appearances": 0.10,
    "rating": 0.20,
    "position": 0.20
  },
  "createdAt": "2026-06-01T00:00:00"
}
```

---

### GET /api/strategies
Todas las estrategias activas (una por tipo).

**Response 200:** `StrategyConfig[]`

---

### GET /api/strategies/{type}
Estrategia activa por tipo.

**Path params:** `GENERAL`, `FORWARD`, `MIDFIELDER`, `DEFENDER`, `GOALKEEPER`

---

### GET /api/strategies/{type}/history
Historial de versiones de una estrategia.

**Response 200:** `StrategyConfig[]`

---

### PUT /api/strategies/{type}
Actualizar estrategia (crea nueva versión).

**Body:**
```json
{
  "valorBase": 12.0,
  "factorEscala": 1.5,
  "weights": {
    "goals": 0.40,
    "assists": 0.10,
    "appearances": 0.10,
    "rating": 0.20,
    "position": 0.20
  }
}
```

**Response 200:** `StrategyConfig` (nueva versión creada)
