# Currency Calculator

A small Spring Boot app for converting between currencies, using live rates from the
[Frankfurter API](https://frankfurter.dev/). Built with Java 21 and Spring Boot 4.1.

## Features

- Convert an amount between any two supported currencies
- Currency list is fetched from Frankfurter and seeded into the database on first run —
  later requests and app restarts read from the database instead of calling the API again
- Exchange rates are cached per currency pair per day, so repeat conversions for the same
  pair don't re-hit the external API
- Frankfurter API calls have connect/read timeouts and retry on transient failures
  (timeouts, connection errors, 5xx responses)
- A simple web page for converting currencies, with a live powers-of-ten reference table

## Requirements

- Java 21
- A PostgreSQL database (no local Maven install needed — use the bundled `./mvnw` wrapper)

## Database setup

The app expects a database named `currency_calculator`, reachable at
`jdbc:postgresql://localhost:5432/currency_calculator`, and a role to connect as:

```sql
CREATE DATABASE currency_calculator;
CREATE USER currency WITH PASSWORD 'currency_dev';
GRANT ALL PRIVILEGES ON DATABASE currency_calculator TO currency;
ALTER DATABASE currency_calculator OWNER TO currency;
```

Connection settings can be overridden via the `DB_USERNAME` and `DB_PASSWORD` environment
variables; see `src/main/resources/application.properties` for defaults.

## Running

```sh
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

## Testing

Tests run against a separate database, so they don't disturb the dev database's data:

```sql
CREATE DATABASE currency_calculator_test;
CREATE USER currency_test WITH PASSWORD 'currency_test';
GRANT ALL PRIVILEGES ON DATABASE currency_calculator_test TO currency_test;
ALTER DATABASE currency_calculator_test OWNER TO currency_test;
```

These credentials are hardcoded in `src/test/resources/application.properties`, which
overrides the main datasource config for the test classpath.

```sh
./mvnw test
```

## API

| Method | Path          | Description                                      |
|--------|---------------|---------------------------------------------------|
| GET    | `/currencies` | List all supported currencies                     |
| GET    | `/currency`   | A currency and its rates to all others; params: `code` (optional, random if omitted) |
| GET    | `/convert`    | Convert an amount; params: `amount`, `from`, `to`  |

Interactive API docs (Swagger UI) are at `/swagger-ui.html`; the raw OpenAPI spec is at
`/v3/api-docs`.
