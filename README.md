# prison-register
Prison registry services for hmpps

[![CircleCI](https://circleci.com/gh/ministryofjustice/prison-register/tree/master.svg?style=svg)](https://circleci.com/gh/ministryofjustice/prison-register)
[![Known Vulnerabilities](https://snyk.io/test/github/ministryofjustice/prison-register/badge.svg)](https://snyk.io/test/github/ministryofjustice/prison-register)

Self-contained fat-jar micro-service to publish prison information
 
### Building

```bash
./gradlew build
```

### Running

```bash
./gradlew bootRun
```

#### Health

- `/health/ping`: will respond with status `UP` to all requests.  This should be used by dependent systems to check connectivity to prison-register,
rather than calling the `/health` endpoint.
- `/health`: provides information about the application health and its dependencies.  This should only be used
by prison-register health monitoring (e.g. pager duty) and not other systems who wish to find out the state of prison-register.
- `/info`: provides information about the version of deployed application.
