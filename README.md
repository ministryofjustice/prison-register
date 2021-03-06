# prison-register
Prison registry services for hmpps

[![CircleCI](https://circleci.com/gh/ministryofjustice/prison-register/tree/master.svg?style=svg)](https://circleci.com/gh/ministryofjustice/prison-register)
[![Known Vulnerabilities](https://snyk.io/test/github/ministryofjustice/prison-register/badge.svg)](https://snyk.io/test/github/ministryofjustice/prison-register)
[![API docs](https://img.shields.io/badge/API_docs_(needs_VPN)-view-85EA2D.svg?logo=swagger)](https://prison-register-dev.hmpps.service.justice.gov.uk/swagger-ui.html)

Self-contained fat-jar micro-service to publish prison information
 
### Building

```bash
./gradlew build
```

### Running

```bash
./gradlew bootRun  --args='--spring.profiles.active=dev' 
```
Which isn't very useful.
Alternatively, to run with a local auth service and postgres first start those docker images using docker-compose
Then run like this:
```bash
./gradlew bootRun --args='--spring.profiles.active=postgres,local' 
```
or
```bash
SPRING_PROFILES_ACTIVE=postgres,local ./gradlew bootRun
```
Swagger documentation is now at http://localhost:8080/swagger-ui.html

You can obtain valid auth tokens form the local auth server using curl.
#### Health

- `/health/ping`: will respond with status `UP` to all requests.  This should be used by dependent systems to check connectivity to prison-register,
rather than calling the `/health` endpoint.
- `/health`: provides information about the application health and its dependencies.  This should only be used
by prison-register health monitoring (e.g. pager duty) and not other systems who wish to find out the state of prison-register.
- `/info`: provides information about the version of deployed application.

### Updating prison information

Use Prison API to make a call to {{prisonapihost}}/api/agencies/type/INST?activeOnly=false to get the list of institutions.
Save this as `all-prisons.json`, then run:
```bash
jq -r '.[] | "INSERT INTO prison VALUES (@" + .agencyId + "@, @" + .description + "@, " + (.active|tostring) + ");"' all-prisons.json | tr @ "'" | sort -k6 > new_prisons.sql
```

This will generate insert statements for each prison, ordered by description.

Now grab all the existing data from the insert statements:
```bash
sort -k6 *insert_prisons.sql | grep 'INSERT INTO prison ' > old_prisons.sql
```

and compare:
```bash
diff -iw old_prisons.sql new_prisons.sql
```

Output the differences to a the new file `VX_X__insert_prisons.sql`.

### Updating GP information

Normally the information will come through in a spreadsheet containing all the prison / gp mappings, not just the new
ones.  If this is not the case then the latest list can be found at https://digital.nhs.uk/services/organisation-data-service/data-downloads/gp-and-gp-practice-related-data - file name epraccur.
Export the provider code and prison code columns to a new file called `codes.csv`.  Then run:
```bash
awk '{print "INSERT INTO prison_gp_practice VALUES (@" $2 "@, @" $1 "@);"}' codes.csv | sort -k 2 | tr @ "'"  > new_gp_practice.sql
```
This will generate insert statements from the codes into `new_gp_practice.sql`.

Now grab all the existing data from the insert statements:
```bash
sort -k5 *insert*.sql | grep 'INSERT INTO prison_gp' > old_gp_practice.sql
```

Then compare to see what's changed:
```bash
diff old_gp_practice.sql new_gp_practice.sql
```
