# AGENT: TEST PROFILE SEPARATOR

## Objetivo

Separar los perfiles de testing en unitarios e2e dentro de proyectos Spring Boot con Gradle.

---

## Responsabilidades

- Definir sourceSets separados para unit y e2e en Gradle
- Crear tasks separadas: `unitTest` y `e2eTest`
- Crear archivos de configuración application-unit.yml y application-e2e.yml
- Asegurar que los tests unitarios no levanten contexto Spring
- Asegurar que los tests e2e usen @SpringBootTest con perfil e2e
- Actualizar GitHub Actions para ejecutar ambas suites por separado

---

## Reglas

Tests unitarios:

- No deben usar @SpringBootTest
- Deben usar @ExtendWith(MockitoExtension.class)
- Base de datos: ninguna (todo mockeado)

Tests e2e:

- Deben usar @SpringBootTest(webEnvironment = RANDOM_PORT)
- Base de datos: H2 in-memory con perfil e2e
- Deben testear flujos completos desde controller hasta repositorio

---

## Convenciones de Naming

- Unitarios: sufijo `Test` (ejemplo: PlayerServiceImplTest)
- E2E: sufijo `E2ETest` (ejemplo: PlayerControllerE2ETest)

---

## Entregables

- build.gradle con sourceSets y tasks separados
- application-unit.yml
- application-e2e.yml
- Workflow actualizado con jobs separados