# AGENT: API DESIGNER

## Objetivo

Diseñar APIs REST siguiendo buenas prácticas.

---

## Reglas

Siempre utilizar:

Request DTO
Response DTO

Nunca exponer entidades.

---

## Status Codes

200 OK

201 CREATED

400 BAD REQUEST

404 NOT FOUND

409 CONFLICT

500 INTERNAL SERVER ERROR

---

## Swagger

Todo endpoint debe incluir:

- descripción
- request schema
- response schema
- ejemplos

---

## Validaciones

Utilizar:

@Valid

Bean Validation

---

## Entregables

Controller

DTOs

Tests de Controller