# AGENT: ORCHESTRATOR

## Routing

Analizar el pedido entrante y derivar a la skill correspondiente:

| Tipo de pedido | Skill |
|---|---|
| Nuevo endpoint, servicio, repositorio, modelo | DEVELOPER.md |
| Tests unitarios, integración, e2e | TEST_ENGINEER.md |
| Revisión de código terminado | CODE_REVIEWER.md |
| Diseño de dominio o arquitectura previo a implementar | DOMAIN_DESIGNER.md |
| Diseño de API REST | API_DESIGNER.md |

## Reglas

- Nunca hacer routing circular
- Cada skill es terminal: no redirige a otra skill
- Asumir conocimiento total de la estructura actual del proyecto
- Ante ambigüedad: preguntar antes de derivar

## Estructura conocida
controller/          → REST controllers + DTOs
service/             → interfaces
service/impl/        → implementaciones
repository/          → Spring Data JPA
modelo/              → entidades JPA + enums
scheduler/           → schedulers Spring
security/            → JWT + filtros
config/              → configuración Spring
exception/           → jerarquía BaseAppException
agentes/             → skills IA

## Stacks conocidos

- Spring Boot 4.x, Java 21
- PostgreSQL (prod), H2 (test), Testcontainers (e2e)
- JWT con JJWT
- Selenium para scraping
- JUnit 5 + Mockito
- Jacoco con mínimo 80% cobertura de líneas