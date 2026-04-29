# Futbol Players Tokens

Proyecto Spring Boot con JWT para validar logins de usuarios.

## Dependencias clave

- Spring Boot 4.0.5
- Spring Web MVC
- Spring Data JPA
- Spring Security
- JJWT 0.11.5
- H2 en memoria para pruebas locales

## Cómo ejecutar

1. Compilar el proyecto:

```bash
./gradlew clean build
```

2. Ejecutar la aplicación:

```bash
java -jar build/libs/futbol-players-tokens-0.0.1-SNAPSHOT.jar --spring.devtools.restart.enabled=false
```

O con Gradle:

```bash
./gradlew bootRun
```

La aplicación se levanta en `http://localhost:8080`.

## Endpoints JWT

### 1. Registro de usuario

- URL: `POST /auth/register`
- Body JSON:
  ```json
  {
    "username": "miusuario",
    "password": "miclave",
    "email": "miemail@example.com"
  }
  ```
- Respuesta exitosa: `User registered successfully`

### 2. Login y obtención de token

- URL: `POST /auth/login`
- Body JSON:
  ```json
  {
    "username": "miusuario",
    "password": "miclave"
  }
  ```
- Respuesta exitosa:
  ```json
  {
    "token": "<jwt-token>"
  }
  ```

### 3. Acceder a rutas protegidas

- Agrega el header HTTP:
  ```text
  Authorization: Bearer <jwt-token>
  ```
- Ejemplo protegido:
  ```bash
  curl -X GET http://localhost:8080/players/1 \
    -H "Authorization: Bearer <jwt-token>"
  ```

## Comportamiento verificado

- Registro de usuario válido ✅
- Login válido y generación de JWT válido ✅
- Acceso a endpoint protegido con JWT válido ✅
- Acceso a endpoint protegido sin token: `403 Forbidden` ✅
- Endpoint protegido con token inválido: `403 Forbidden` ✅

## Notas importantes

- El JWT se genera con una clave secreta fija en `JwtUtil`.
- En producción, reemplaza `SECRET_KEY` por una clave segura y administra la configuración con variables de entorno.
- El proyecto usa H2 en memoria para pruebas locales, por lo que los datos se pierden al reiniciar la aplicación.
