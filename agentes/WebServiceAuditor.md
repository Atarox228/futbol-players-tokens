# AGENT: WEB SERVICE AUDITOR

## Objetivo

Implementar auditoría de todos los servicios REST publicados: loguear timestamp, usuario, operación/método, parámetros y tiempo de ejecución usando Spring AOP + Logback.

---

## Contexto

Repositorio con:

- agentes/AGENTS.md — reglas globales
- 8 controllers en com.desapp.futbolplayerstokens.controller
- Seguridad JWT con SecurityContextHolder
- Logging actual: @Slf4j y LoggerFactory.getLogger (sin config Logback explícita)

---

## Tareas

### 1. LoggingAspect

Paquete: com.desapp.futbolplayerstokens.config

- @Aspect @Component
- @Around apuntando a todos los métodos públicos de controllers
- Loggear con SLF4J:
  - timestamp ISO-8601
  - usuario (SecurityContextHolder o "anonymous")
  - operación (HTTP method + path, extraído de HttpServletRequest)
  - método Java ejecutado
  - parámetros de request sanitizados
  - tiempo de ejecución en ms
- Usar MDC para requestId (UUID por request)

### 2. Sanitización

- Ocultar: password, secret, token, authorization
- Truncar parámetros > 500 chars
- No loguear body de POST/PUT completos cuando excedan 1000 chars

### 3. Logback

Crear src/main/resources/logback-spring.xml

- Console appender con patrón incluyendo MDC
- File appender rotativo:
  - archivo: logs/audit.log
  - maxHistory: 30 días
  - maxFileSize: 100MB
- Logger específico para el aspect con nivel INFO

### 4. Dependencia

Agregar en build.gradle si no existe:

implementation 'org.springframework.boot:spring-boot-starter-aop'

### 5. Tests

- LoggingAspectTest con MockMvc
- Verificar formato del log generado
- Verificar sanitización de datos sensibles

---

## Entregables

LoggingAspect.java en config/

logback-spring.xml en resources/

build.gradle actualizado

Tests del aspect

---

## Reglas

- No exponer datos sensibles en logs
- Usar MDC para contexto de request
- El aspect no debe alterar la respuesta del controller
- Bajo overhead
