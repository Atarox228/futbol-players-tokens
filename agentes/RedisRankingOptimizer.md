# AGENT: REDIS RANKING OPTIMIZER

## Objetivo

Optimizar el endpoint GET /players/ranking para alta frecuencia de consultas implementando Redis como caché.

---

## Contexto

Repositorio con:

- agentes/AGENTS.md — reglas globales
- RankingServiceImpl — paginación custom con subList entre non-null y null scores
- RankingService — interfaz del servicio
- PlayerControllerREST.getRanking() — endpoint GET /players/ranking
- docker-compose.yml — PostgreSQL + App
- Sin caché ni Redis actualmente

---

## Tareas

### 1. Dependencia Redis

Agregar en build.gradle:

implementation 'org.springframework.boot:spring-boot-starter-data-redis'

### 2. Configuración

- application.yml: spring.data.redis host/port
- docker-compose.yml: agregar servicio redis:7, puerto 6379

### 3. RedisConfig

Paquete: com.desapp.futbolplayerstokens.config

- RedisTemplate<String, List<PlayerRankingDTO>>
- TTL configurable via @Value (default 5 min)

### 4. Modificar RankingServiceImpl

Cache-aside pattern:

- getRanking(): consultar Redis primero, si miss → DB → guardar en Redis
- Key: "ranking:{page}:{size}"
- TTL: 5 minutos

### 5. Invalidación

Al actualizar scores desde QuoteScheduler o scraper:

- Eliminar todas las keys "ranking:*" de Redis

### 6. Tests

- Actualizar RankingServiceImplTest con mock de RedisTemplate
- Test de cache hit vs miss

---

## Entregables

build.gradle actualizado

RedisConfig.java

docker-compose.yml actualizado

application*.yml actualizados

RankingServiceImpl modificado

Tests actualizados

---

## Reglas

- No hardcodear TTL ni conexión
- Usar perfiles Spring para separar entornos
- Redis solo para producción/dev
