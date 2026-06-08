# API Reference — FutbolTokens

## Base URL

```
http://localhost:8080
```

Autenticación vía JWT (cookie `jwt` o header `Authorization: Bearer <token>`).

---

## Auth (`/auth`)

### `POST /auth/register`

Registrar un nuevo usuario.

```json
{ "username": "string", "password": "string", "email": "string" }
```

**Respuesta:** `200 OK`

---

### `POST /auth/login`

Iniciar sesión.

```json
{ "username": "string", "password": "string" }
```

**Respuesta:**
```json
{ "token": "jwt...", "message": "Login successful", "userId": 1 }
```

---

### `POST /auth/logout`

Cerrar sesión (invalida cookie).

**Respuesta:**
```json
{ "message": "Logout successful" }
```

---

## Players (`/players`)

### `GET /players/hello`

Health check.

**Respuesta:** `"Hello World"`

---

### `GET /players`

Listar jugadores con filtros opcionales.

| Parámetro | Tipo | Requerido |
|-----------|------|-----------|
| `league` | String | No |
| `team` | String | No |
| `position` | String | No |

**Respuesta:** `PlayerDTO[]`

---

### `GET /players/ranking`

Ranking paginado ordenado por score.

| Parámetro | Tipo | Default |
|-----------|------|---------|
| `page` | int | 0 |
| `size` | int | 20 |

**Respuesta:** `PlayerRankingDTO[]`

---

### `GET /players/{id}`

Detalle de un jugador.

**Respuesta:** `PlayerDetailDTO`

---

### `GET /players/{id}/quotes`

Historial de cotizaciones de un jugador.

**Respuesta:** `QuoteDTO[]`

---

### `POST /players/scrape`

Scrapeo forzado completo de todos los jugadores.

**Respuesta:**
```json
"✓ Scraping forzado completado correctamente\n⏱️ Tiempo total: X min Y seg"
```

---

### `POST /players/scrape/new-only`

Scrapeo incremental solo de jugadores nuevos.

**Respuesta:**
```json
"✓ Se encontraron y guardaron N jugadores NUEVOS de todas las ligas\n⏱️ Tiempo total: X min Y seg"
```

---

### `POST /players/scrape/team/{id}`

Scrapear y sobrescribir jugadores de un equipo por su `TeamEnum` ID.

**Respuesta:**
```json
"✓ Scraping completado para <team>. Jugadores procesados. Tiempo: X min Y seg"
```

---

### `POST /players/scrape/league/{league}`

Scrapear todos los equipos de una liga.

Valores: `LaLiga`, `Premier League`, `Bundesliga`, `Serie A`, `Ligue 1`

**Respuesta:**
```json
"✓ Scraping completo de <league> finalizado (todos los equipos). Tiempo: X min Y seg"
```

---

## Orders (`/orders`)

### `POST /orders/buy`

Crear orden de compra.

```json
{ "playerId": 1, "quantity": 10, "idempotencyKey": "uuid", "maxPrice": 50.0 }
```

**Respuesta:** `OrderDTO`

---

### `POST /orders/sell`

Crear orden de venta.

```json
{ "playerId": 1, "quantity": 10, "idempotencyKey": "uuid", "minPrice": 50.0 }
```

**Respuesta:** `OrderDTO`

---

### `GET /orders/transactions`

Transacciones paginadas del usuario autenticado.

| Parámetro | Tipo | Default |
|-----------|------|---------|
| `page` | int | 0 |
| `size` | int | 20 |

**Respuesta:** `Page<OrderDTO>`

---

### `GET /orders/book`

Order book, opcionalmente filtrado por tipo.

| Parámetro | Tipo | Requerido |
|-----------|------|-----------|
| `type` | `BUY` / `SELL` | No |
| `page` | int | No |
| `size` | int | No |

**Respuesta:** `Page<OrderDTO>`

---

### `GET /orders/pending`

Órdenes pendientes del usuario autenticado.

| Parámetro | Tipo | Requerido |
|-----------|------|-----------|
| `type` | `BUY` / `SELL` | No |
| `page` | int | No |
| `size` | int | No |

**Respuesta:** `Page<OrderDTO>`

---

### `POST /orders/sell-all`

Vender todos los tokens del usuario autenticado.

**Respuesta:** `OrderDTO[]`

---

### `GET /orders/player/{playerId}`

Órdenes abiertas (PENDING + PARTIALLY_FILLED) de un jugador.

**Respuesta:** `OrderDTO[]`

---

### `POST /orders/{id}/cancel`

Cancelar una orden propia.

**Respuesta:** `OrderDTO`

---

## Quotes (`/quotes`)

### `POST /quotes/recalculate`

Recalcular cotizaciones de todos los jugadores.

**Respuesta:** `202 Accepted`

---

### `GET /quotes/player/{id}/current`

Cotización actual de un jugador.

**Respuesta:** `QuoteDTO`

---

## Portfolio (`/users/{id}`)

### `GET /users/{id}/portfolio`

Portfolio paginado de un usuario.

| Parámetro | Tipo | Default |
|-----------|------|---------|
| `page` | int | 0 |
| `size` | int | 20 |

**Respuesta:** `Page<PortfolioDTO>`

---

### `GET /users/{id}/transactions`

Todas las transacciones de un usuario.

**Respuesta:** `OrderDTO[]`

---

### `GET /users/{id}/balance`

Balance del usuario.

**Respuesta:**
```json
{ "userId": 1, "username": "string", "balance": 1000.0 }
```

---

## Matches (`/api/matches`)

### `GET /api/matches/all`

Todos los partidos registrados.

**Respuesta:** `MatchDTO[]`

---

### `GET /api/matches/{id}`

Partido por ID.

**Respuesta:** `MatchDTO`

---

### `GET /api/matches/team/{teamId}`

Partidos de un equipo.

**Respuesta:** `MatchDTO[]`

---

### `POST /api/matches`

Crear partido.

```json
{ "footballDataMatchId": 123, "team1Id": 1, "team2Id": 2, "matchTime": "2026-06-08T20:00:00" }
```

**Respuesta:** `201 Created` → `MatchDTO`

---

### `PUT /api/matches/{id}`

Actualizar partido.

**Respuesta:** `MatchDTO`

---

### `DELETE /api/matches/{id}`

Eliminar partido.

**Respuesta:** `204 No Content`

---

### `POST /api/matches/scrape/today`

Raspar partidos de hoy desde fuente externa.

**Respuesta:** `MatchDTO[]`

---

## Scheduler (`/api/scheduler`)

### `GET /api/scheduler/status`

Estado del scheduler.

**Respuesta:**
```json
{ "status": "ACTIVE", "matchCount": 5 }
```

---

### `GET /api/scheduler/matches`

Partidos programados.

**Respuesta:**
```json
{ "matches": [...], "count": 5 }
```

---

### `POST /api/scheduler/test/schedule-all`

Programar todos los partidos (testing).

**Respuesta:**
```json
{ "message": "Scheduled N matches", "count": 5 }
```

---

## Strategies (`/api/strategies`)

### `GET /api/strategies/active`

Estrategia activa más reciente.

**Respuesta:** `StrategyConfig`

---

### `GET /api/strategies`

Todas las estrategias activas (una por tipo).

**Respuesta:** `StrategyConfig[]`

---

### `GET /api/strategies/{type}`

Estrategia activa por tipo.

Tipos: `GENERAL`, `FORWARD`, `MIDFIELDER`, `DEFENDER`, `GOALKEEPER`

**Respuesta:** `StrategyConfig`

---

### `GET /api/strategies/{type}/history`

Historial de versiones de una estrategia.

**Respuesta:** `StrategyConfig[]`

---

### `PUT /api/strategies/{type}`

Actualizar estrategia (crea nueva versión).

```json
{ "valorBase": 10.0, "factorEscala": 2.5, "weights": { "goals": 0.3, "assists": 0.2 } }
```

**Respuesta:** `StrategyConfig`

---

## DTOs

### OrderDTO

| Campo | Tipo |
|-------|------|
| id | Long |
| userId | Long |
| playerId | Long |
| playerName | String |
| type | `BUY` / `SELL` |
| quantity | int |
| priceAtOrder | BigDecimal |
| total | BigDecimal |
| idempotencyKey | String |
| createdAt | LocalDateTime |
| status | `PENDING` / `PARTIALLY_FILLED` / `FILLED` / `CANCELLED` |
| remainingQuantity | int |

### PortfolioDTO

| Campo | Tipo |
|-------|------|
| playerId | Long |
| playerName | String |
| tokenQty | int |
| avgBuyPrice | BigDecimal |
| currentValue | BigDecimal |
| profitLoss | BigDecimal |
| currentPrice | BigDecimal |

### PlayerDTO

| Campo | Tipo |
|-------|------|
| id | Long |
| name | String |
| team | String |
| league | String |
| position | String |
| score | BigDecimal |
| altPosition | String |

### PlayerDetailDTO

| Campo | Tipo |
|-------|------|
| id | Long |
| name | String |
| rating | Double |
| team | String |
| league | String |
| position | String |
| appearances | Integer |
| minutes | Integer |
| goals | Integer |
| assists | Integer |
| shotsOnTarget | Double |
| passAccuracy | Double |
| aerialWon | Double |
| faults | Double |
| offsidesGiven | Double |
| clears | Double |
| dribbled | Double |
| tackles | Double |
| interceptions | Double |
| blocks | Double |
| ownGoals | Integer |
| keyPasses | Double |
| dribbles | Double |
| faulted | Double |
| offsides | Double |
| dispossesed | Double |
| turnover | Double |
| yellowCards | Integer |
| redCards | Integer |
| playerOfTheMatch | Integer |
| score | BigDecimal |
| altPosition | String |

### PlayerRankingDTO

| Campo | Tipo |
|-------|------|
| playerId | Long |
| rank | int |
| score | BigDecimal |
| playerName | String |
| team | String |
| position | String |
| league | String |
| altPosition | String |

### QuoteDTO

| Campo | Tipo |
|-------|------|
| id | Long |
| playerId | Long |
| price | BigDecimal |
| timestamp | LocalDateTime |
| strategyId | Long |
| strategyVersion | Integer |
| trigger | String |

### MatchDTO

| Campo | Tipo |
|-------|------|
| id | Long |
| footballDataMatchId | Long |
| team1Id | Long |
| team2Id | Long |
| matchTime | LocalDateTime |
