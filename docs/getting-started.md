# Getting started

For a developer who knows HMPPS (HMPPS Auth, Cloud Platform, Helm, DPS) but has not worked on the
registers before. The commands here have been run as written; if one does not work, fix this page.

The service is two repositories. Clone both side by side:

```bash
git clone git@github.com:ministryofjustice/prison-register.git     # the API (this repository)
git clone git@github.com:ministryofjustice/hmpps-registers.git      # the front end
```

## The API (prison-register)

### Prerequisites

| Tool | Version | Notes |
| --- | --- | --- |
| Java | **25** | `build.gradle.kts` sets the Kotlin toolchain to 25. Gradle downloads nothing for you if 25 is missing; install it (for example Amazon Corretto 25). |
| Docker | any current version | For Postgres, LocalStack and HMPPS Auth. |

### Run it

```bash
TMPDIR=/private$TMPDIR docker compose -f docker-compose-local.yml up -d
./gradlew bootRun --args='--spring.profiles.active=dev'
```

`docker-compose-local.yml` starts everything the API needs except the API itself:

| Container | Port | For |
| --- | --- | --- |
| `prison-register-db` | 5432 | Postgres 18, database, user and password all `prison-register` |
| `localstack` | 4566 | SNS (domain events) and SQS (audit) |
| `hmpps-auth` | 8090 | Tokens, if you want to call secured endpoints |

The `dev` profile (`application-dev.yml`) also switches on the `localstack` profile, which creates
the topic and queue with random names at start-up. Flyway creates the schema on first start.

Check it is up:

- <http://localhost:8080/health>: every component should be `UP`
- <http://localhost:8080/prisons>: about 170 prisons from the migrations, no token needed
- <http://localhost:8080/swagger-ui/index.html>: the API documentation

`docker-compose.yml` has the same three containers plus the API itself, built from the
`Dockerfile` and run with host networking. It can be handy when you are working on the front end and
only need the API running, but the `bootRun` route above is the one that is kept working.

### What the local database holds

Only what the Flyway migrations insert: the prisons and their reference data. **There are no
courts, hospitals or other agencies locally.** Those were loaded into each environment from NOMIS by
the migration described in [process-flows.md](process-flows.md#migration-and-reconciliation). To
work on them locally, create a few through the API or the front end, or test against dev.

### Calling secured endpoints

Most endpoints need a token with a role (see [architecture.md](architecture.md#roles)). The local
HMPPS Auth container issues tokens, but its clients do not have the registers roles, so a token
from it gets `403` on `/courts`, `/api/agencies` and the like. In practice:

- use the integration tests, which mint tokens with any role through `JwtAuthorisationHelper`; or
- call the dev environment with a dev client that has the role.

### Tests

```bash
./gradlew test       # unit and integration tests
./gradlew check      # tests plus ktlint and the other checks CI runs
./gradlew ktlintFormat
```

The integration tests use **Testcontainers**: they start Postgres and LocalStack themselves. You do
not need `docker-compose-test.yml`, whatever older notes say. But read the next section.

### The port 8090 trap

The integration tests start a WireMock stand-in for HMPPS Auth on **port 8090**
(`wiremock/HmppsAuthMockServer.kt`). That is the same port as the `hmpps-auth` container in the
local compose file. If the container is running, about half the test classes fail at start-up
with `Failed to bind to /0.0.0.0:8090` (reported as `initializationError`). Stop it first:

```bash
docker stop hmpps-auth
./gradlew test
```

### The port 5432 trap

The test set-up (`PostgresContainer.kt`, `LocalStackContainer.kt`) checks whether **anything** is
listening on port 5432 or 4566. If something is, it does not start a container and uses whatever is
there, with user, password and database all `prison-register`.

- If the local compose stack is running, the tests use its database. That works, but your local
  data and the tests share a database.
- If **another project's** Postgres is on 5432 (several HMPPS projects use that port), the tests
  connect to it and fail with authentication or missing-database errors that have nothing to do
  with your change.

Stop other Postgres containers before running the tests, or run them with nothing on 5432 so they
start their own.

## The front end (hmpps-registers)

See the "Front end" section in [architecture.md](architecture.md#the-front-end) for how it is put
together.

### Prerequisites

| Tool | Version | Notes |
| --- | --- | --- |
| Node | **24** (`.nvmrc`) | `nvm use` in the repository root. `.npmrc` sets `engine-strict = true`, so the wrong major version fails the install outright. |
| npm | 11 or 12 | Comes with Node 24. |
| Docker | any current version | For Redis, and WireMock for the Cypress tests. |
| gitleaks | any | `brew install gitleaks`. The pre-commit hook needs it. |

### Install

```bash
nvm use
npm run setup
```

Use `npm run setup`, **not** `npm install`. `.npmrc` sets `ignore-scripts = true`;
`npm run setup` runs `npm ci` and then only the install scripts allowed in `.allowed-scripts.mjs`
(Cypress, esbuild and a few others). Those entries are pinned to exact versions, so when Renovate
bumps one of those packages `.allowed-scripts.mjs` has to change in the same pull request.

### Run it against the dev environment

This is the normal way to work on the front end: your local app, with dev HMPPS Auth and the dev
API.

```bash
docker compose -f docker-compose-test.yml up -d redis     # Redis on 6379
cp .env.example .env                                       # then fill in the blanks, see below
npm run start:dev
```

The app is on <http://localhost:3000>. Sign in with a dev account that has
`ROLE_HMPPS_REGISTERS_MAINTAINER`.

- **Redis is always needed**, even though `.env.example` says `REDIS_ENABLED=false`. That setting
  only moves sessions into memory; the token cache always uses Redis (`server/index.ts`). Without
  it every page that calls the API fails.
- **Fill in the blanks in `.env`.** `API_CLIENT_ID`, `API_CLIENT_SECRET`, `SYSTEM_CLIENT_ID`,
  `SYSTEM_CLIENT_SECRET` and `SESSION_SECRET` are empty in the template. **An empty value is
  treated as not set**, so the app silently falls back to the built-in defaults
  (`hmpps-registers-ui-client` / `clientsecret`) and sign-in fails with no useful message. The dev
  values are in the `hmpps-registers` secret in the `hmpps-registers-dev` namespace.
- **`.env` must exist.** `start:dev` runs Node with `--env-file=.env`, which stops if the file is
  missing. Only `start:dev` and the `start-feature` scripts read env files; `npm start` does not.

### Running the front end against a local API

Possible, but rarely worth it. A local API (`dev` profile) only trusts tokens signed by the
**local** HMPPS Auth key, so pointing `PRISON_REGISTER_API_URL` at `http://localhost:8080` while
signing in through dev HMPPS Auth gets a 401 from the API, and the front end then signs you out
(see [common-problems.md](common-problems.md)). To work across both, change the API, test it with
its integration tests, deploy it to dev, then work on the front end against dev.

Do not use the front end's `docker-compose.yml` to run the whole stack. Its `app` service sets
environment variable names the code does not read and runs with `NODE_ENV=production`, so it stops
at start-up with `Missing env var API_CLIENT_ID`.

### Tests

```bash
npm run lint
npm run typecheck
npm test                  # Jest unit tests, no build or Redis needed
```

Cypress integration tests run the **built** app against WireMock, so build first:

```bash
docker compose -f docker-compose-test.yml up -d    # Redis 6379, WireMock 9091
npm run build                                      # required: start-feature runs dist/
npm run start-feature                              # app on 3007, every API pointed at WireMock
npm run int-test                                   # or int-test-ui for the Cypress window
```

`npm run start-feature:dev` instead of the build and start steps rebuilds as you change code.
Stubs are in `integration_tests/mockApis/` and are registered as Cypress tasks in
`integration_tests/plugins/index.ts`.

> `docker-compose-test.yml` gives its containers fixed names (`roll-redis`, `wiremock`) and fixed
> ports. If another project already has a container with that name, or something on 9091 or 6379,
> start fails with a name or port conflict. Stop the other one first.

The pre-commit hook runs gitleaks, lint-staged, the full type check and the full unit test suite,
so commits are slow.

## Troubleshooting

See [common-problems.md](common-problems.md).
