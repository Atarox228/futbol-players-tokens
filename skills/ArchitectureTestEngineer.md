# AGENT: ARCHITECTURE TEST ENGINEER

## Objetivo

Crear tests de arquitectura que validen la estructura del proyecto y prevengan degradación arquitectónica usando ArchUnit.

---

## Tecnologías

ArchUnit (archunit-junit5:1.3.0+)

JUnit 5

---

## Responsabilidades

Diseñar e implementar tests que verifiquen:

- Separación de capas (controller → service → repository)
- Dependencias correctas entre paquetes
- Naming conventions existentes del proyecto
- Ausencia de dependencias cíclicas
- Restricciones de acceso (no lógica en controllers/repositories)

---

## Reglas de Arquitectura a Validar

### Capas y dependencias

- controller solo depende de: service, controller.dto, exception, modelo, repository, security
- service solo depende de: repository, modelo, controller.dto
- service.impl solo depende de: service, repository, modelo
- repository solo depende de: modelo
- modelo no depende de ninguna capa superior

### Naming

- Controllers: sufijo *Controller o *ControllerREST
- Services: interfaz con sufijo *Service, impl con sufijo *Impl
- Repositories: sufijo *Repository
- DTOs: sufijo *DTO, *Request, *Response
- Exceptions: sufijo *Exception

### Prohibiciones

- No usar @Autowired (usar constructor injection)
- No lógica de negocio en controllers
- No lógica de negocio en repositories
- No System.out.println ni System.err.println

---

## Implementación

Crear archivo: src/test/java/com/desapp/futbolplayerstokens/architecture/ArchitectureTest.java

Usar @AnalyzeClasses(packages = "com.desapp.futbolplayerstokens")

---

## Dependencia

Agregar en build.gradle:

testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'

---

## Verificación

- Ejecutar ./gradlew test
- Todos los architecture tests deben pasar
- Cobertura JaCoCo debe mantenerse en 80%
