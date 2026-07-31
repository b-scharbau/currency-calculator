# Currency Calculator

[![CI](https://github.com/b-scharbau/currency-calculator/actions/workflows/ci.yml/badge.svg)](https://github.com/b-scharbau/currency-calculator/actions/workflows/ci.yml)

**Live**: [currency.bscharbau.com](https://currency.bscharbau.com)

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
- An Angular frontend for converting currencies, with a live powers-of-ten reference table

## Requirements

- Java 21
- A PostgreSQL database (no local Maven install needed — use the bundled `./mvnw` wrapper)
- No local Node/npm/Angular CLI needed — the Maven build installs its own (see Frontend below)

## Database setup

The app expects a database named `currency_calculator`, reachable at
`jdbc:postgresql://localhost:5432/currency_calculator`, and a role to connect as:

```sql
CREATE DATABASE currency_calculator;
CREATE USER currency_dev WITH PASSWORD 'currency_dev';
GRANT ALL PRIVILEGES ON DATABASE currency_calculator TO currency_dev;
ALTER DATABASE currency_calculator OWNER TO currency_dev;
```

Connection settings can be overridden via the `DB_USERNAME` and `DB_PASSWORD` environment
variables; see `src/main/resources/application.properties` for defaults.

## Running

```sh
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. This also builds the Angular frontend (see below) —
first run will take longer while it downloads Node and npm packages.

## Frontend

The UI lives in `frontend/`, a standalone Angular 20 app (standalone components, Signals) that
talks to the REST API below. `./mvnw spring-boot:run` / `package` build it automatically via
`frontend-maven-plugin`, which downloads its own pinned Node/npm — nothing needs to be
preinstalled, and the compiled output lands directly in `target/classes/static` (never committed
to source control).

For backend-only iteration, skip the frontend build:

```sh
./mvnw spring-boot:run -Dfrontend.skip=true
```

For frontend-only iteration with live reload against a running backend:

```sh
cd frontend
npm install
npm start   # ng serve, proxying /currencies, /currency, /convert to localhost:8080
```

This needs a local Node matching `frontend/.nvmrc` (use nvm/volta, or `frontend/node/` if you've
already run a Maven build once, since frontend-maven-plugin leaves its downloaded Node there).

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

Angular's own unit tests (`frontend/src/**/*.spec.ts`) run separately, headless via Chrome:

```sh
cd frontend
CHROME_BIN=$(which chromium || which google-chrome) npm test -- --watch=false --browsers=ChromeHeadless
```

## API

| Method | Path          | Description                                      |
|--------|---------------|---------------------------------------------------|
| GET    | `/currencies` | List all supported currencies                     |
| GET    | `/currency`   | A currency and its rates to all others; params: `code` (optional, random if omitted) |
| GET    | `/convert`    | Convert an amount; params: `amount`, `from`, `to`  |

Interactive API docs (Swagger UI) are at `/swagger-ui.html`; the raw OpenAPI spec is at
`/v3/api-docs`.

## Deployment

The app runs in production on AWS (ECS Fargate behind an ALB, at `https://currency.bscharbau.com`),
provisioned via Terraform in `infra/` (local state, not committed — see `infra/*.tf` for the full
resource layout: ECR, ACM, ALB, ECS, IAM, the SSM-stored DB password, and the additive
Route53/security-group records into the existing shared `bscharbau.com` zone and RDS instance).

Initial infrastructure setup (`terraform apply` + the one-off database bootstrap below) has already
been done.

**Deploys are automatic**: the `deploy` job in `.github/workflows/ci.yml` builds and pushes a new
image to ECR and rolls the ECS service on every push to `master`, once the `backend` and `frontend`
test jobs pass. It authenticates to AWS via OIDC (`infra/github_oidc.tf`) — a role trusted only for
this exact repo on `refs/heads/master`, scoped to just pushing this one ECR repo and updating this
one ECS service. No AWS credentials are stored in GitHub.

To deploy manually (e.g. testing an image before pushing to master):

```sh
# 1. Build and push a new image
docker build -t 768664385987.dkr.ecr.ap-northeast-1.amazonaws.com/currency-calculator:latest .
aws ecr get-login-password --region ap-northeast-1 | \
  docker login --username AWS --password-stdin 768664385987.dkr.ecr.ap-northeast-1.amazonaws.com
docker push 768664385987.dkr.ecr.ap-northeast-1.amazonaws.com/currency-calculator:latest

# 2. Roll the ECS service to pick up the new image
aws ecs update-service --cluster currency-calculator --service currency-calculator \
  --force-new-deployment --region ap-northeast-1

# 3. Confirm it landed
aws ecs wait services-stable --cluster currency-calculator --service currency-calculator \
  --region ap-northeast-1
curl -I https://currency.bscharbau.com/
```

Infrastructure changes go through Terraform (`cd infra && terraform plan` — review carefully, since
this account also hosts unrelated infrastructure — then `terraform apply`).

**Database bootstrap** (already done once; only needed again if the prod database/role is ever
recreated). The app's own password lives in SSM as a Terraform-generated secret, not something to
retype by hand:

```sh
aws ssm get-parameter --name /currency-calculator/db-password --with-decryption \
  --region ap-northeast-1 --query 'Parameter.Value' --output text
```

Then, connecting to the shared RDS instance as the master user (from a host inside the VPC — the
existing `bscharbau-com-server` EC2 box works as a network jump point and has `psql` installed):

```sql
CREATE DATABASE currency_calculator;
CREATE USER currency_prod WITH PASSWORD '<value fetched above>';
GRANT ALL PRIVILEGES ON DATABASE currency_calculator TO currency_prod;
ALTER DATABASE currency_calculator OWNER TO currency_prod;
```
