# API Reference — Futbol Players Tokens

## Autenticación

### POST /auth/register
Registrar un nuevo usuario.

**Request:**
```json
{
  "username": "juan_perez",
  "password": "password123",
  "email": "juan@example.com"
}
```
**Response 200:** `"User registered successfully"`
**Response 400:** mensaje de error

### POST /auth/login
Iniciar sesión. Devuelve JWT en el body y en una cookie `authToken`.

**Request:**
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
**Response 400:** `"Invalid credentials"`

### POST /auth/logout
Invalida la cookie `authToken`.

**Response 200:**
```json
{ "message": "Logout successful" }
```

---

## Players (`/players`)

### GET /players/hello
Health check público.

**Response 200:** `"Hello World"`

### GET /players
Lista de jugadores con filtros opcionales.

**Query params:** `league`, `team`, `position`

**Response 200:**
```json
[
  {
    "id": 1,
    "name": "Cristiano Ronaldo",
    "team": "Al Nassr",
    "league": "Saudi League",
    "position": "Forward",
    "score": 85.50,
    "altPosition": "Midfielder"
  }
]
```

### GET /players/ranking
Ranking paginado de jugadores por puntuación.

**Query params:** `page` (default 0), `size` (default 20)

**Response 200:**
```json
[
  {
    "id": 1,
    "name": "Jugador",
    "score": 85.50,
    "position": "Forward",
    "team": "Equipo"
  }
]
```

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
  "altPosition": "Midfielder"
}
```
**Response 404:** si no existe

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

### POST /players/scrape
Scrapeo forzado completo de todas las ligas (público, puede tardar minutos).

**Response 200:** mensaje con tiempo total
**Response 500:** mensaje de error

### POST /players/scrape/new-only
Scrapea solo jugadores nuevos no existentes en la BD (público).

**Response 200:** mensaje con cantidad de nuevos jugadores
**Response 500:** mensaje de error

### POST /players/scrape/team/{id}
Scrapea y sobreescribe jugadores de un equipo por ID de `TeamEnum`.

**Response 200:** mensaje con tiempo total
**Response 400:** ID inválido

### POST /players/scrape/league/{league}
Scrapea todos los equipos de una liga. Ligas soportadas: `LaLiga`, `Premier League`, `Bundesliga`, `Serie A`, `Ligue 1`.

**Response 200:** mensaje con tiempo total
**Response 400:** liga inválida

---

## Usuarios (`/users`) — requiere autenticación

### GET /users/{id}/portfolio
Portfolio del usuario (paginado). El usuario autenticado debe coincidir.

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
  "totalPages": 1
}
```

### GET /users/{id}/transactions
Historial de transacciones del usuario.

**Response 200:**
```json
[
  {
    "id": 1,
    "userId": 1,
    "playerId": 5,
    "playerName": "Messi",
    "type": "BUY",
    "quantity": 5,
    "priceAtOrder": 1.50,
    "total": 7.50,
    "idempotencyKey": "key-123",
    "createdAt": "2026-06-01T10:30:00",
    "status": "FILLED",
    "remainingQuantity": 0
  }
]
```

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

## Órdenes (`/orders`) — requiere autenticación

### POST /orders/buy
Crear orden de compra.

**Request:**
```json
{
  "playerId": 5,
  "quantity": 5,
  "idempotencyKey": "uuid-unico",
  "maxPrice": 100.00
}
```
**Response 200:** `OrderDTO` con `type: "BUY"`

### POST /orders/sell
Crear orden de venta.

**Request:**
```json
{
  "playerId": 5,
  "quantity": 5,
  "idempotencyKey": "uuid-unico",
  "minPrice": 1.00
}
```
**Response 200:** `OrderDTO` con `type: "SELL"`

### GET /orders/transactions
Transacciones del usuario autenticado (paginado, ordenado por `createdAt`).

### GET /orders/book
Libro de órdenes abiertas de todos los usuarios.

**Query params:** `type` (BUY/SELL, opcional)

### GET /orders/pending
Órdenes pendientes del usuario autenticado.

**Query params:** `type` (BUY/SELL, opcional)

### POST /orders/sell-all
Vende todos los tokens del usuario autenticado.

**Response 200:** `List<OrderDTO>`

### GET /orders/player/{playerId}
Órdenes asociadas a un jugador.

**Response 200:** `List<OrderDTO>`

### POST /orders/{id}/cancel
Cancela una orden pendiente.

**Response 200:** `OrderDTO`

---

## Cotizaciones (`/quotes`)

### POST /quotes/recalculate
Recalcula cotizaciones de todos los jugadores manualmente.

**Response 202:** `"Recalculation triggered for all players"`

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

## Partidos (`/api/matches`)

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
    "matchTime": "2026-06-09T15:00:00",
    "team1Name": null,
    "team2Name": null
  }
]
```

### GET /api/matches/{id}
Partido por ID.

### GET /api/matches/team/{teamId}
Partidos de un equipo.

### POST /api/matches
Crear un nuevo partido.

**Request:**
```json
{
  "footballDataMatchId": 12345,
  "team1Id": 10,
  "team2Id": 20,
  "matchTime": "2026-06-09T15:00:00"
}
```
**Response 201:** `MatchDTO`

### PUT /api/matches/{id}
Actualizar un partido.

**Response 200:** `MatchDTO`
**Response 404:** si no existe

### DELETE /api/matches/{id}
Eliminar un partido.

**Response 204:** sin contenido

### POST /api/matches/scrape/today
Scrapea partidos del día desde fuente externa.

**Response 200:** `List<MatchDTO>`

---

## Scheduler (`/api/scheduler`)

### GET /api/scheduler/status
Estado actual del programador de partidos.

**Response 200:**
```json
{
  "scheduledMatches": 5,
  "status": "ACTIVE"
}
```

### GET /api/scheduler/matches
Partidos programados actualmente.

**Response 200:**
```json
{
  "total": 5,
  "matches": [ ... ]
}
```

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

## Estrategias (`/api/strategies`)

### GET /api/strategies/active
Estrategia activa global (última versión general).

**Response 200:** `StrategyConfig` con pesos, tipo y factor de escala.

### GET /api/strategies
Todas las estrategias activas (una por tipo).

**Response 200:** `List<StrategyConfig>`

### GET /api/strategies/{type}
Estrategia activa por tipo. Tipos: `GENERAL`, `FORWARD`, `MIDFIELDER`, `DEFENDER`, `GOALKEEPER`.

### GET /api/strategies/{type}/history
Historial de versiones de una estrategia.

### PUT /api/strategies/{type}
Actualizar estrategia (crea nueva versión).

**Request:**
```json
{
  "weights": {
    "goals": 0.35,
    "assists": 0.15
  }
}
```
**Response 200:** nuevo `StrategyConfig`
