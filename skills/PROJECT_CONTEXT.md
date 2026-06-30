# Project Context

This file captures the current repository layout and build conventions so future requests can be answered against the real structure of the project.

## Stack

- Java 21
- Spring Boot 4.0.5
- Gradle
- PostgreSQL in runtime, H2 for local tests, Testcontainers for E2E tests
- JWT with JJWT
- Selenium for scraping
- Jacoco + SonarQube

## Current Source Layout

- `src/main/java/com/desapp/futbolplayerstokens/controller` for REST controllers and DTOs
- `src/main/java/com/desapp/futbolplayerstokens/service` for service interfaces
- `src/main/java/com/desapp/futbolplayerstokens/service/impl` for service implementations
- `src/main/java/com/desapp/futbolplayerstokens/repository` for Spring Data repositories
- `src/main/java/com/desapp/futbolplayerstokens/modelo` for entities and enums
- `src/main/java/com/desapp/futbolplayerstokens/security` for JWT and security config
- `src/main/java/com/desapp/futbolplayerstokens/config` for Spring configuration
- `src/main/java/com/desapp/futbolplayerstokens/exception` for application exceptions
- `src/main/java/com/desapp/futbolplayerstokens/scheduler` for scheduled jobs

## Test Layout

- All tests currently live under `src/test/java`
- E2E tests are under `src/test/java/com/desapp/futbolplayerstokens/e2e`
- There is no active `src/e2e` source set in the build
- Repository tests also live under `src/test/java/com/desapp/futbolplayerstokens/repository`

## Build Conventions

- `test` task is disabled because the project uses custom `unitTest` and `e2eTest` tasks
- `unitTest` runs non-Spring tests
- `e2eTest` runs `@SpringBootTest` tests with the `e2e` profile
- Jacoco reports are generated from `build/jacoco/unitTest.exec` and `build/jacoco/e2eTest.exec`
- Coverage excludes configuration, DTOs, entities, schedulers, and scraper implementations

## Useful Commands

- `./gradlew help` to validate build configuration
- `./gradlew unitTest` to run unit tests
- `./gradlew e2eTest` to run integration and E2E tests
- `./gradlew jacocoAggregateReport` to generate the combined coverage report
- `./gradlew check` to run the verification pipeline

## Notes For Future Changes

- Keep `AGENTS.md` aligned only when the folder structure or routing rules change
- Avoid reintroducing stale `src/e2e` or `src/test/repository` paths unless the layout is intentionally changed
- Mockito is available transitively through `spring-boot-starter-test`, so extra Mockito dependencies are usually unnecessary