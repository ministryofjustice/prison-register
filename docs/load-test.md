# Load test

The register will carry about twice today's traffic once services move off Prison API's
`/api/agencies` (MAPB-1129). The load test in `src/gatling` sends the prod mix of calls at a
multiple of the expected peak, so that the pod count, connection pool and database size can be
set from measurements rather than guesses.

It is a [Gatling](https://docs.gatling.io/) simulation written in Kotlin, run through Gradle. Nothing
else needs installing.

## What it sends

Each endpoint has its own share of the traffic, taken from prod over the 30 days to 13 November
2026. The first group is what the register gets today. The second is the Prison API `/api/agencies`
traffic that will move to the register. The rates are for the default steady run, which is twice
the peak.

| Endpoint | Share | Requests a second at 2x the peak |
| --- | ---: | ---: |
| `POST /prisons/prisonsByIds` (1 to 5 prisons) | 31.4% | 110 |
| `GET /prisons/names?active=true` | 5.3% | 19 |
| `GET /prisons/names?prison_id={id}` | 2.0% | 7 |
| `GET /prisons/names` | 0.7% | 2 |
| `GET /prisons/id/{id}` | 5.5% | 19 |
| `GET /prisons` | 0.8% | 3 |
| `GET /secure/prisons/id/{id}/department/contact-details` | 0.3% | 1 |
| `GET /legacy/reconciliation/{id}` (only with `INCLUDE_SYNC=true`) | 0.3% | 1 |
| `GET /api/agencies/{id}` (9 in 10 for a prison) | 43.1% | 151 |
| `GET /api/agencies/legacy-type/SCH` | 5.3% | 19 |
| `GET /api/agencies/legacy-type/INST` | 5.1% | 18 |
| `GET /api/agencies/legacy-type/HOSPITAL` and `HSHOSP` | 0.1% | 0.5 |

Where the numbers come from:

- **Peak:** the busiest minute in those 30 days for the register and the Prison API calls that will
  move, added together, was 10,490 calls. The test uses 10,500 a minute, which is 175 a second, so
  2x is 350 a second.
- **Register:** 15.5 million calls. Health checks are left out.
- **Prison API:** 18 million calls to `/api/agencies/{id}`, `/api/agencies/type/{type}`,
  `/api/agencies/prison` and `/api/agencies/prisons`. Callers that move will use `legacy-type` with
  the NOMIS code they use today. `eventLocations`, `receptionsWithCapacity` and `pay-profile` are
  left out: that is not register data and it will not move here.
- The prison and agency ids are read from the environment under test when the run starts.

The test makes no changes. A change to the register clears every cache on the pod that makes it,
and the next reads on that pod go to the database. Changes are a few a week, so this is not part of
the test.

## Before you run it

- **Use preprod, in working hours.** The API in preprod is switched off from 21:30 to 06:30 UTC and
  at weekends.
- **Preprod is smaller than prod.** It has 2 pods (prod has 3) and a `db.t4g.micro` database (prod
  has a `db.t4g.small`). A pass in preprod is a safe sign for prod; a failure needs checking against
  the difference.
- **Tell the teams that use the register in preprod** before a steady or stress run. A stress run
  is meant to find the point where it fails.
- **Get a client for the test.** It needs a preprod HMPPS Auth client with the
  `HMPPS_REGISTERS_API__R` role, plus `HMPPS_REGISTERS_API__SYNCHRONISATION__RW` if you include the
  NOMIS reconciliation calls. Use a client of its own rather than another service's, so the test
  traffic can be told apart in App Insights.

## Running it

Set the client details, then pick a profile. Start with a smoke run to check the set-up.

```bash
export CLIENT_ID=... CLIENT_SECRET=...

# 5 requests a second for 1 minute
PROFILE=smoke ./gradlew gatlingRun --simulation uk.gov.justice.digital.hmpps.prisonregister.loadtest.RegisterLoadSimulation

# Ramp to 2x the peak over 5 minutes, then hold it for 20 minutes
PROFILE=steady ./gradlew gatlingRun --simulation uk.gov.justice.digital.hmpps.prisonregister.loadtest.RegisterLoadSimulation

# Step up by one peak (175 a second) every 3 minutes, to 5x the peak
PROFILE=stress ./gradlew gatlingRun --simulation uk.gov.justice.digital.hmpps.prisonregister.loadtest.RegisterLoadSimulation
```

| Variable | Default | What it does |
| --- | --- | --- |
| `PROFILE` | `smoke` | `smoke`, `steady` or `stress`. |
| `BASE_URL` | preprod | The API to test. |
| `AUTH_URL` | preprod HMPPS Auth token URL | Where to get a token. |
| `CLIENT_ID`, `CLIENT_SECRET` | none | A client credentials token is fetched with these and renewed every 10 minutes. |
| `AUTH_TOKEN` | none | Use this token instead. It must last the whole run. |
| `PEAK_PER_MINUTE` | `10500` | The peak the multipliers apply to. |
| `MULTIPLIER` | `2` | Steady: how many times the peak to hold. |
| `RAMP_MINUTES`, `HOLD_MINUTES` | `5`, `20` | Steady: how long to ramp up and how long to hold. |
| `MAX_MULTIPLIER`, `STEP_MINUTES` | `5`, `3` | Stress: how far to go and how long each step lasts. |
| `INCLUDE_SYNC` | `false` | Include the NOMIS reconciliation calls. Needs the synchronisation role. |

A laptop on the MoJ network can send 350 requests a second without difficulty.

## Pass marks

The steady profile fails the build unless, as set out in MAPB-1129:

- fewer than 0.1% of requests fail, and
- 95% of `GET /api/agencies/{id}` calls take under 200 ms.

The smoke profile fails on any failed request. The stress profile has no pass mark.

Response times are measured from where the test runs, so they include the network. Compare them
with the times the API records in App Insights.

## What to watch during a run

The Gatling report shows the client's side. Watch the server's side too:

- **App Insights:** `requests` for `cloud_RoleName == "prison-register"`, failures and duration by
  endpoint.
- **Pods:** `kubectl -n hmpps-registers-preprod top pods` for memory and CPU, and
  `kubectl -n hmpps-registers-preprod get pods` for restarts.
- **Database:** the CloudWatch metrics for the preprod instance (`cloud-platform-897c419584ae8631`):
  CPU, connections and, because it is a burstable class, CPU credit balance.

The report is written to `build/reports/gatling/`. Record the results and the settings used on
MAPB-1129.
