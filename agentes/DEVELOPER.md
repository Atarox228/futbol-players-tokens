# AGENT: DEVELOPER

## Objetivo

Implementar funcionalidad nueva o modificar existente respetando la arquitectura del proyecto.

## Proceso obligatorio

1. Identificar entidades involucradas en `modelo/`
2. Verificar repositorios existentes en `repository/`
3. Verificar si existe interfaz en `service/` antes de crear implementación
4. Implementar en `service/impl/`
5. Exponer en `controller/` usando DTOs exclusivamente — nunca exponer entidades
6. Registrar excepciones usando la jerarquía existente en `exception/`

## Reglas

- Lógica exclusivamente en services
- Controllers solo reciben request, delegan y devuelven response
- Repositories solo persisten — sin lógica de negocio
- DTOs obligatorios en entrada y salida de controllers
- No hardcodear valores configurables
- No duplicar código existente — verificar reutilización antes de crear

## Convenciones del proyecto

- Entidades usan `@Builder`, `@Getter`, `@Setter`, Lombok
- `@PrePersist` / `@PreUpdate` para defaults en entidades
- Excepciones heredan de `BaseAppException` con `HttpStatus`
- Perfiles: `test`, `e2e`, producción sin perfil explícito
- Schedulers en `scheduler/` con `@Profile("!test & !e2e")`
- Seguridad stateless JWT — no sessions

## Entregables

- Interfaz en `service/` si no existe
- Implementación en `service/impl/`
- Controller actualizado o nuevo
- DTOs request/response
- Imports completos
- Sin tests — derivar a TEST_ENGINEER.md