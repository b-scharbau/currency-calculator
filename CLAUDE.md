# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`currency-calculator` is a Spring Boot 4.1.0 (Java 21) web app that converts between currencies using live
rates from the [Frankfurter API](https://frankfurter.dev/), with a Postgres-backed cache and an Angular
frontend styled to match [bscharbau.com](https://bscharbau.com). Group/artifact: `com.bscharbau:currency-calculator`.

## Commands

Use the Maven wrapper (`mvnw`); no local Maven install is required. A running Postgres instance is required
for compile/run/test (see Database below) — `./mvnw compile` alone doesn't need it, but `spring-boot:run`
and `test` do.

- Build: `./mvnw compile`
- Run tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=ExchangeRateServiceTest`
- Package: `./mvnw package`
- Run the app locally: `./mvnw spring-boot:run` (serves on `http://localhost:8080`)
- Skip the Angular build for backend-only iteration: append `-Dfrontend.skip=true` to any of the above

## Database

Postgres is required. Two separate databases/roles are used — see `README.md` for the exact `CREATE
DATABASE`/`CREATE USER` statements:

- **Dev**: `currency_calculator`, role `currency` — configured in `src/main/resources/application.properties`
  (credentials overridable via `DB_USERNAME`/`DB_PASSWORD` env vars).
- **Test**: `currency_calculator_test`, role `currency_test` — configured in
  `src/test/resources/application.properties`, which overrides the main datasource config on the test
  classpath so `./mvnw test` never touches dev data.

Schema is managed via `spring.jpa.hibernate.ddl-auto=update` (Hibernate auto-creates/updates tables from the
`@Entity` classes) — there are no migration scripts.

## Architecture

### Controllers
- `CurrencyController` — `GET /currency` (a specific currency via `?code=`, or random if omitted, with its
  rates to every other currency as `rates: [{to, rate}, ...]`) and `GET /currencies` (full supported list).
- `CurrencyConversionController` — `GET /convert?amount=&from=&to=`, converts an amount between two
  currencies.
- All REST controllers are standalone top-level classes in `com.bscharbau.currencycalculator` using
  `@RestController`. `CurrencyCalculatorApplication` is only the `@SpringBootApplication` entry point —
  don't nest controllers inside it.

### Services
- `CurrencyService` — seeds the `currency` table from Frankfurter's `/currencies` on first run only
  (`@PostConstruct`, guarded by `repository.count() > 0`); `fetchCurrencies()` always reads from the DB.
- `ExchangeRateService` — `convert()` for a single pair and `ratesFor()` for all rates of a base currency.
  Both check a DB cache first and only call Frankfurter if the cached entry isn't from today.

### Caching
Two separate cache entities, both keyed by "fetched today or not" (`fetchedAt` field, distinct from
`rateDate`, which is Frankfurter's own as-of date and can lag on weekends/holidays):
- `CachedExchangeRate` (+ `CachedExchangeRateRepository`) — one row per `(from, to)` pair, used by `/convert`.
- `CachedCurrencyRates` (+ `CachedCurrencyRatesRepository`) — one row per base currency, rates stored via a
  JPA `@ElementCollection` (`cached_currency_rate_entry` join table), used by `/currency`.

These are independent caches (a `/convert` call doesn't warm the `/currency` cache or vice versa).

### External API access
- `FrankfurterClientConfig` provides a single shared `RestClient` bean (`frankfurterRestClient`) pointed at
  `https://api.frankfurter.dev/v1`, with a 3s connect timeout and 5s read timeout via
  `JdkClientHttpRequestFactory`. Inject this bean rather than creating a new `RestClient`.
- `FrankfurterRetry.execute(Supplier<T>)` wraps Frankfurter calls with up to 3 attempts and increasing
  backoff, retrying `ResourceAccessException` (timeouts/connection errors) and `HttpServerErrorException`
  (5xx). `HttpClientErrorException` (4xx, e.g. unknown currency code) is deliberately not retried — it's
  caught by the caller and translated into a `ResponseStatusException(BAD_REQUEST)`.

### OpenAPI
`OpenApiConfig` sets API metadata; `springdoc-openapi-starter-webmvc-ui` auto-generates docs from the
controllers. Swagger UI at `/swagger-ui.html`, raw spec at `/v3/api-docs`. Add `@Operation`/`@Parameter`
annotations on new endpoints so the generated docs stay useful.

### Frontend
`frontend/` is a standalone Angular 20 app (standalone components, no NgModules; reactive state via
Signals) — a separate npm/TypeScript project, sibling to `src/`, not part of the Maven module.

- `CurrencyApi` (`frontend/src/app/currency-api.ts`) wraps `HttpClient`: `getCurrencies()` (`GET
  /currencies`) and `getCurrencyRates(code)` (`GET /currency?code=`), typed to match the backend DTOs
  (`Currency`, `RateEntry`, `CurrencyRates`) exactly.
- `Calculator` (`frontend/src/app/calculator/calculator.ts`) owns all the interactive behavior, carried over
  unchanged from the original hand-written page: it fetches `/currency?code=<from>` once per "from" change
  (incl. initial load and swap) and holds that response in a `currentRates` signal; `computed()` signals
  derive the conversion result and the powers-of-ten table from `currentRates()`/`to()`/`amount()` with zero
  further HTTP calls — changing "to" or typing an amount is instant. `/convert` exists as a backend endpoint
  but the frontend doesn't call it.
- `App` (`frontend/src/app/app.ts`) is just the page shell (nav/hero/footer) hosting `<app-calculator>`.
- Global styles (`frontend/src/styles.css`) hold `bscharbau.com`'s design system: `--paper`/`--ink`/
  `--signal`/`--muted`/`--line`/`--tint` custom properties, Space Grotesk + IBM Plex Sans/Mono, and the
  shared nav/hero/section-head/panel/meta-strip/table component classes.

**Build integration**: `frontend-maven-plugin` (in `pom.xml`, bound to the `generate-resources` phase —
before Maven's `process-resources` copies `src/main/resources/**`) downloads its own pinned Node/npm
(independent of any system Node) and runs `npm install` + `npm run build`. `frontend/angular.json`'s
`outputPath` is set to `{"base": "../target/classes/static", "browser": ""}` so Angular's build lands flat
there directly — nothing under `src/main/resources/static` exists in source control; it's populated purely
by this build step. `-Dfrontend.skip=true` skips all three plugin executions.

For local Angular-only dev with live reload against a real backend: `cd frontend && npm start` runs `ng
serve` with `frontend/proxy.conf.json` proxying `/currencies`, `/currency`, `/convert` to
`localhost:8080` — this is dev-only convenience, never used in the actual build.

## Testing

The suite spans three levels, all under `src/test/java/com/bscharbau/currencycalculator/`:
- **Unit tests** (e.g. `ExchangeRateServiceTest`, `CurrencyServiceTest`, `FrankfurterRetryTest`) — no Spring
  context; HTTP is stubbed with `MockRestServiceServer` bound to a manually-built `RestClient`, repositories
  are Mockito mocks.
- **Controller slice tests** (`@WebMvcTest`, e.g. `CurrencyControllerTest`) — needs the
  `spring-boot-webmvc-test` dependency (Spring Boot 4 split `@WebMvcTest` out of `spring-boot-starter-test`);
  mock service beans with `@MockitoBean` (not the deprecated `@MockBean`).
- **Repository tests** (`@DataJpaTest` + `@AutoConfigureTestDatabase(replace = Replace.NONE)`, e.g.
  `CurrencyRepositoryTest`) — needs the `spring-boot-data-jpa-test` dependency; runs against the real
  `currency_calculator_test` database (no embedded DB on the classpath), with per-test transaction rollback.
  Use currency codes that aren't real ISO 4217 codes (e.g. `ZZA`/`ZZB`) in test fixtures to avoid colliding
  with real seeded data.

The Angular app has its own separate suite under `frontend/src/**/*.spec.ts` (Karma/Jasmine): a
`CurrencyApi` spec asserting the right URLs are hit, and a `Calculator` spec covering the derived-conversion
math and — mirroring the backend's cache-hit-avoids-network-call philosophy — asserting that changing "to"
or the amount triggers zero additional HTTP requests while changing "from" does. Run via `cd frontend &&
CHROME_BIN=$(which chromium) npm test -- --watch=false --browsers=ChromeHeadless`.
