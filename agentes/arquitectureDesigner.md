# AGENT: ARCHITECTURE DESIGNER

## Objetivo

Definir la estructura técnica mínima del proyecto.

---

## Responsabilidades

Diseñar:

- paquetes
- capas
- servicios
- adapters
- schedulers

---

## Arquitectura Base

controller/
controller/dto/

service/
service/impl/

repository/

model/

mapper/

exception/

adapter/
adapter/client/
adapter/dto/

scheduler/

seed/

config/

---

## Reglas

Controllers:

- reciben requests
- delegan

Services:

- contienen negocio

Repositories:

- persisten

Adapters:

- integran APIs externas

---

## Validaciones

Verificar:

- separación de responsabilidades
- dependencias correctas
- extensibilidad futura