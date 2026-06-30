# AGENT: TEST ENGINEER

## Objetivo

Generar pruebas automáticas para código nuevo o existente.

## Tecnologías del proyecto

- JUnit 5
- Mockito
- Spring Boot Test + MockMvc
- H2 en memoria para perfil `test`
- Testcontainers + PostgreSQL real para perfil `e2e`
- `@SpringBootTest` + `@ActiveProfiles("test")` para tests de integración
- `@Tag("e2e")` obligatorio en tests e2e

## Tipos de test y qué mockear

### Unit (sin Spring context)
- Usar `@ExtendWith(MockitoExtension.class)`
- Mockear: repositories, services externos, adapters
- No mockear: lógica propia del servicio bajo prueba
- Cubrir: caso feliz, errores esperados, edge cases

### Controller (sin Spring context)
- Usar `@ExtendWith(MockitoExtension.class)` sobre el controller directamente
- Validar: status codes, payloads, errores, delegación correcta

### Repository (con H2)
- Usar `@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional`
- Solo cuando existen queries custom `@Query`, JPQL o filtros
- Verificar orden, filtros y resultados

### E2E (con Testcontainers)
- Extender `AbstractE2ETest`
- Usar `@WithMockUser` para endpoints autenticados
- Usar `@Transactional` para aislar estado
- Verificar flujo completo HTTP → DB

## Cobertura mínima

80% de líneas — configurado en Jacoco.

Exclusiones ya configuradas en `build.gradle`:
- `config/`, `exception/`, `controller/dto/`, `modelo/`, `scheduler/`
- `PlayerScraperServiceImpl`, `MatchScraperServiceImpl`

## Convenciones

- `@MockitoBean` para schedulers en `@SpringBootTest`
- `lenient()` para stubs no usados en todos los tests
- Nombre de test: `metodoBajoTest_escenario_resultadoEsperado`

## Entregables

- Tests unitarios para todo método público nuevo
- Tests de controller para todo endpoint nuevo
- Tests de repository para queries custom nuevas
- Tests e2e para flujos críticos nuevos