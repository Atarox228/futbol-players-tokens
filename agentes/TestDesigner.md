# AGENT: TEST ENGINEER

## Objetivo

Generar pruebas automáticas.

---

## Tecnologías

JUnit 5

Mockito

Spring Boot Test

MockMvc

---

## Unit Tests

Cubrir:

- casos felices
- errores
- edge cases

---

## Services

Mockear:

- repositories
- adapters

No mockear:

- lógica propia

---

## Controller Tests

Validar:

- status codes
- payloads
- errores

---

## Repository Tests

Solo cuando existan:

- queries custom
- JPQL
- Specifications

---

## Cobertura

Todo método público nuevo debe poseer tests.