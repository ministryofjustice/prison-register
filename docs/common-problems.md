# Common problems

Problems people actually hit, and what to do. If you hit a new one and work it out, add it here.

## Running locally

| Symptom | Cause and fix |
| --- | --- |
| API tests: about half the classes fail with `initializationError` and `Failed to bind to /0.0.0.0:8090` | The `hmpps-auth` container from `docker-compose-local.yml` is on 8090, where the tests start their WireMock HMPPS Auth. `docker stop hmpps-auth`, then run the tests again. |
| API tests fail with Postgres authentication errors or "database does not exist" | Another project's Postgres is on 5432. The tests use whatever is listening there instead of starting their own. Stop it. See [getting-started.md](getting-started.md#the-port-5432-trap). |
| API test waiting for a message times out (`ConditionTimeoutException ... within 10 seconds`) | A timing flake in tests that wait for an event on the LocalStack queue. Run the class again on its own; if it passes, it was the flake. |
| API starts but `/courts` and similar are empty locally | Expected. Only prisons are in the migrations; other agencies were loaded into each environment from NOMIS. Create some, or use dev. |
| A local token gets `403` from the API | The local HMPPS Auth clients do not have the registers roles. Use the integration tests or dev. |
| Front end: every page that calls the API errors or hangs | Redis is not running. It is needed even with `REDIS_ENABLED=false`. `docker compose -f docker-compose-test.yml up -d redis`. |
| Front end: sign-in fails with no clear message | The client ids and secrets in `.env` are blank, so the app used its built-in defaults. Fill them in from the `hmpps-registers` secret in dev. |
| Front end: `start:dev` stops immediately with an env file error | There is no `.env`. `cp .env.example .env`. |
| Front end: you are signed out as soon as a page loads data | The API returned 401 or 403, and the front end turns any 401 or 403 into a sign-out. Usually the front end is pointed at a local API while signing in through dev HMPPS Auth (the local API does not trust dev tokens), or its client lacks a role. |
| Front end: `npm ci` fails with an engine error | Wrong Node version. `nvm use`. |
| Front end: Cypress is missing or will not start | You ran `npm install`. Run `npm run setup`. |
| Cypress fails on code you just changed | `start-feature` runs the built code in `dist/`. Run `npm run build`, or use `start-feature:dev`. |
| `docker compose up` in the front end fails with `Missing env var API_CLIENT_ID` | The front end's `docker-compose.yml` is out of date and does not work. Run the app with `npm run start:dev` instead. |
| Container name `roll-redis` or `wiremock` already in use, or port 9091 or 6379 taken | Another project's containers. Stop them before starting the front end's test stack. |

## In dev and preprod

| Symptom | Cause and fix |
| --- | --- |
| The front end loads but every page with data fails, in the evening or at a weekend | The API in dev and preprod is switched off from 21:30 to 06:30 UTC and at weekends. See [deployment.md](deployment.md#environments). |
| Something you set up in preprod has gone | Preprod's database is refreshed from production. |
| Swagger UI is not available in preprod or prod | It is only switched on in dev: <https://prison-register-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html>. |
| A change to areas, regions or the town list does not show in the front end | The front end keeps reference data in memory until its pods restart. |
| The home page does not show a register | `AGENCY_REGISTER_ENABLED` is off for that environment. |
| There is no delete button | `AGENCY_REGISTER_DELETE_ENABLED` is off. It is off in production on purpose. |

## Data and NOMIS

| Symptom | Cause and fix |
| --- | --- |
| A prison's name in DPS does not match NOMIS | Expected. DPS prison names were standardised on purpose and are not synced either way. |
| A change to a prison's description in DPS is overwritten | The description comes from NOMIS, so each sync from NOMIS replaces it. |
| A change to a prison in DPS does not reach NOMIS | Only the prison's area is sent to NOMIS. Everything else about a prison stays in DPS. |
| A change to a court, hospital or other agency does not reach NOMIS | Check that `prisoner-to-nomis-update` received the `register.*` event and look for errors in its logs, and in its dead letter queue. Events with source `NOMIS` are ignored on purpose. |
| A new prison created in NOMIS is missing types, categories, operator or address | Expected. The sync creates a skeleton prison; a central admin completes it in the front end. See [process-flows.md](process-flows.md#creating-a-new-prison). |
| A caller of `/api/agencies` gets `PRISON` where it expected `INST` | The register uses its own type names. Use `/api/agencies/legacy-type/{nomisCode}` to ask by NOMIS code, and map the type in the caller. |
| A caller needs a prison's business phone number | The register does not record phone number types, so it cannot say which number is the business line. |

## Known issues

These are open problems in the code, recorded here so nobody loses a day finding them again.

- **The prison add journey does not check the code against other agency types.** The six other
  add journeys call `GET /legacy/agency/{id}/is-used`; the prison one only checks other prisons.
- **Adding a prison without answering the long-term high-security question** is not stopped by
  validation (`addNewPrisonDetailsValidator.ts`), and the summary step then calls
  `form.lthse.includes(...)` on the missing answer (`addNewPrisonSummaryValidator.ts`). This looks
  likely to give an error page; it has not been confirmed by running it.
- **`GET /api/agencies` reads every agency table in full on each call.** Fine for occasional use;
  callers should cache it.
- **`DELETE /legacy/admin/sync/agency/all` is not limited to dev.** It deletes every non-prison
  agency. Only the sync role can call it.
- **The front end's README and `docker-compose.yml` are out of date** (CircleCI, `npm install`, a
  changelog that does not exist, a compose `app` service that cannot start). Trust these docs over
  them.
