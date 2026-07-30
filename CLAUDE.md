# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`currency-calculator` is a Spring Boot 4.1.0 (Java 21) web application, currently in early skeleton stage — no currency-calculation logic has been implemented yet. Group/artifact: `com.bscharbau:currency-calculator`.

## Commands

Use the Maven wrapper (`mvnw`); no local Maven install is required.

- Build: `./mvnw compile`
- Run tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=CurrencyCalculatorApplicationTests`
- Package: `./mvnw package`
- Run the app locally: `./mvnw spring-boot:run`

## Architecture

- `CurrencyCalculatorApplication` (`src/main/java/com/bscharbau/currencycalculator/CurrencyCalculatorApplication.java`) is the `@SpringBootApplication` entry point. It currently also declares a nested `HelloController` (`GET /hello`) directly inside the application class rather than as a separate file.
- Other REST controllers, such as `HealthController` (`GET /health`, returns `{"status":"UP"}`), are defined as standalone top-level classes in the same package (`com.bscharbau.currencycalculator`) using `@RestController`. Prefer this standalone-class style for new endpoints rather than nesting inside the application class.
- No persistence, service, or currency-conversion layers exist yet — only `spring-boot-starter` and `spring-boot-starter-web` are on the classpath (plus `spring-boot-starter-test` for tests). Any data lookups, external rate providers, or business logic will need to be introduced as this project grows.
- Configuration lives in `src/main/resources/application.properties` (currently only sets `spring.application.name`).
