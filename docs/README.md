# HMPPS Registers: developer documentation

HMPPS Registers is where DPS keeps its list of **agencies**: prisons, courts, hospitals, probation
offices, police custody suites, approved premises and every other kind of place a prisoner can be
sent to or come from. Central administrators maintain the list; other HMPPS services read it. It
replaces the agency maintenance screens in NOMIS, which are now blocked for users.

The service is two applications, and these documents cover both:

| Application | Repository | What it is |
| --- | --- | --- |
| **prison-register** | this repository | Kotlin / Spring Boot API. Owns the database, publishes events, syncs with NOMIS. |
| **hmpps-registers** | [hmpps-registers](https://github.com/ministryofjustice/hmpps-registers) | Node / TypeScript front end for central administrators. |

If you have worked on other HMPPS services, the things to know first:

- **The API is called prison-register but holds every agency type.** It began as a register of
  prisons and kept the name.
- **NOMIS is still in the loop.** Changes in the register are sent to NOMIS, and changes in NOMIS
  come back, through two Syscon services. Prisons follow different rules from every other agency
  type. See [process-flows.md](process-flows.md).
- **Prison data can be read without a token.** `GET /prisons/**` is open; everything else needs a
  role.

## Where to start

New to the service? Read **getting started, then architecture, then process flows**. The rest are
reference.

| Document | Read this when... |
| --- | --- |
| [getting-started.md](getting-started.md) | You want the API and the front end running locally with tests passing. |
| [architecture.md](architecture.md) | You need the shape of the service: components, layers, roles, events, sign-in. |
| [process-flows.md](process-flows.md) | You need to know how a change travels between the front end, the API, NOMIS and the services that read the register. |
| [data-model.md](data-model.md) | You are writing a query or a migration. |
| [deployment.md](deployment.md) | You are shipping, or looking for a namespace, secret or alert. |
| [common-problems.md](common-problems.md) | Something is not working, or you want the known issues. |

## Repository layout (API)

| Path | What lives there |
| --- | --- |
| `src/main/kotlin/.../prisonregister/resource/` | REST controllers and DTOs, one per agency type, plus `PublicApiResource` and `LegacySyncResource`. |
| `src/main/kotlin/.../prisonregister/service/` | Business rules, domain events (`SnsService`) and audit (`AuditService`). |
| `src/main/kotlin/.../prisonregister/model/` | JPA entities and repositories. |
| `src/main/resources/db/migration/` | Flyway migrations. |
| `async-api.yml`, `async-schemas/` | The domain events the API publishes. |
| `helm_deploy/` | Helm chart and per-environment values. |
| `buildSrc/` | Gradle tasks for port-forwarding to RDS and reading secrets. |
| `docs/` | You are here. |

## Useful links

- API documentation (dev, needs VPN): <https://prison-register-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html>
- Domain events: [`async-api.yml`](../async-api.yml)
- Front end (dev): <https://registers-dev.hmpps.service.justice.gov.uk>
- Service overview for non-developers (Confluence): <https://dsdmoj.atlassian.net/wiki/spaces/4410474729/pages/6366888063>
