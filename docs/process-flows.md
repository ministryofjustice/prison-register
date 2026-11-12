# Process flows

How data moves between the front end, the API, NOMIS and the services that read the register.

The one rule that explains most of this page: **every change raises a domain event that says where
it came from** (`additionalInformation.source` is `DPS` or `NOMIS`). The two Syscon sync services
use that field to stop changes bouncing back and forth.

## A change made in DPS

A central admin user changes a court's phone number in the front end.

```mermaid
sequenceDiagram
    actor User as Central admin user
    participant UI as hmpps-registers (front end)
    participant API as prison-register (API)
    participant DB as Postgres
    participant Audit as HMPPS Audit (SQS)
    participant Topic as Domain events (SNS)
    participant ToNomis as prisoner-to-nomis-update
    participant NOMIS

    User->>UI: Save phone number
    UI->>API: PUT /courts/id/{courtId}/phone-number/{phoneNumberId}
    API->>DB: Update
    API->>Audit: Audit event (who, what, when)
    API->>Topic: register.court.phone.amended, source = DPS
    API-->>UI: 200
    Topic->>ToNomis: event
    ToNomis->>API: Read the court
    ToNomis->>NOMIS: Update the agency's phone
```

Every agency type works this way, with events named `register.<type>.<change>` (for example
`register.hospital.address.inserted`, `register.agency.deleted`). The full list is in
`async-api.yml` and `service/SnsService.kt`.

**Prisons are the exception.** For a prison, only a change of **area** is sent to NOMIS
(`register.prison.area.amended`). Prison names, active flags and contact details changed in DPS stay
in DPS. Prison names in particular are deliberately different from NOMIS; see
[data-model.md](data-model.md#things-that-surprise-people).

## A change made in NOMIS

Until NOMIS is switched off, some agency data still changes in NOMIS (for example by Syscon
scripts). The NOMIS agency maintenance screens themselves are now blocked for users.

```mermaid
sequenceDiagram
    participant NOMIS
    participant FromNomis as prisoner-from-nomis-migration
    participant API as prison-register (API)
    participant Topic as Domain events (SNS)
    participant ToNomis as prisoner-to-nomis-update

    NOMIS->>FromNomis: AGENCY_LOCATIONS-UPDATED (or PHONES_AGENCY-*, ADDRESSES_AGENCY-* ...)
    FromNomis->>NOMIS: Read the whole agency
    FromNomis->>API: POST /legacy/sync/agency/id/{agencyId}<br/>(or /legacy/sync/prison/id/{agencyId})
    API->>API: Create or update
    API->>Topic: register.<type>.amended, source = NOMIS
    Topic->>ToNomis: event
    ToNomis->>ToNomis: source is NOMIS, so ignore
```

Points to know:

- The sync sends the **whole agency** each time, and the API creates it if it does not exist. It
  is an "upsert", so repeating it is harmless.
- `AGENCY_LOCATIONS-DELETED` is not supported. Agencies are made inactive, not deleted.
- For prisons, the sync takes the NOMIS-owned fields (such as description, area and CJIT code) and
  leaves DPS-owned fields alone. `Prison.update` has name and active commented out on purpose.
- The `/legacy/**` endpoints need `ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW`, which only the
  Syscon sync clients should hold.

## Creating a new prison

Prisons are created in NOMIS first, by a Syscon script, and arrive in the register through the
sync. They arrive incomplete, so a central admin user finishes them in the front end.

```mermaid
flowchart LR
    script["Syscon script<br/>creates the prison in NOMIS"] --> sync["from-NOMIS sync<br/>POST /legacy/sync/prison/id/{id}"]
    sync --> skeleton["Prison in the register<br/>name, description, active,<br/>area, CJIT code only"]
    skeleton --> admin["Central admin completes it<br/>in the front end: types, categories,<br/>operator, address, departments"]
    admin --> events["register.prison.* events<br/>(only area goes back to NOMIS)"]
```

The front end still has an "Add a new prison" journey. Use it only if you know the prison already
exists in NOMIS or is about to; the later sync updates the same row rather than creating a second
one. Do not add new prisons by Flyway migration.

## Migration and reconciliation

The non-prison agencies were loaded from NOMIS by a one-off migration run from
`prisoner-from-nomis-migration`, using `POST /legacy/migrate/agency/id/{agencyId}`. Migration does
not raise domain events.

`prisoner-to-nomis-update` runs a reconciliation that compares NOMIS with the register, using:

| Endpoint | Returns |
| --- | --- |
| `GET /legacy/reconciliation/ids/all` | The ids of every non-prison agency |
| `GET /legacy/reconciliation/prison/ids/all` | The ids of every prison |
| `GET /legacy/reconciliation/{agencyId}` | One agency, in the NOMIS-shaped `LegacyAgencyDto` |

Differences are reported to Application Insights. The reconciliation stops at the first difference
for each agency, so it understates how many fields differ.

> **`DELETE /legacy/admin/sync/agency/all` deletes every non-prison agency.** It exists to reset an
> environment before a fresh migration. Nothing in the code limits it to dev. Only the sync role can
> call it; do not give that role to anything else.

## How other services read the register

```mermaid
flowchart LR
    subgraph readers["Other HMPPS services"]
        dps["DPS front ends and APIs"]
        legacy["Services moving off<br/>Prison API /api/agencies"]
        slm["Send Legal Mail and others"]
    end

    dps -->|"GET /prisons/... (no token needed)"| api["prison-register"]
    slm -->|"GET /prisons/..."| api
    legacy -->|"GET /api/agencies/...<br/>ROLE_HMPPS_REGISTERS_API__R"| api
    api -.->|"register.* events"| topic["Domain events topic"]
    topic -.-> subs["Subscribers that cache<br/>register data"]
```

| Endpoint group | Use it for | Role |
| --- | --- | --- |
| `GET /prisons`, `/prisons/id/{id}`, `/prisons/names`, `/prisons/search`, `POST /prisons/prisonsByIds` | Prison details for DPS screens and letters. | None. `/prisons/**` is open to any caller that can reach the service. |
| `GET /api/agencies`, `/api/agencies/{id}`, `/api/agencies/type/{type}`, `/api/agencies/legacy-type/{nomisCode}` | Any agency type, in a shape close to Prison API's `/api/agencies`. The replacement for Prison API agency lookups. | `ROLE_HMPPS_REGISTERS_API__R` |
| `GET /secure/prisons/id/{prisonId}/department/contact-details?departmentType=...` | A prison department's phone, email and web address. | Any valid HMPPS Auth token. Changing them needs `ROLE_MAINTAIN_REF_DATA` or `ROLE_MAINTAIN_PRISON_DATA` with write scope. |

`/api/agencies/{id}` returns an agency whether it is active or not. The list endpoints return
active agencies only, unless called with `activeOnly=false`. The `/api/agencies` list endpoint reads
every agency table in full on each call, so callers should cache the answer rather than call it per
request.
