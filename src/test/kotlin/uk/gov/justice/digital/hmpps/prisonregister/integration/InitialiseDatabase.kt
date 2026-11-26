package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.junit.jupiter.api.Test

/**
 * Builds the schema so the SchemaSpy report and the CSV data dictionary can be generated from it.
 *
 * Left out of normal test runs; run with `./gradlew -Pinit-db=true test` (see build.gradle.kts and
 * .github/workflows/schema-spy.yml). Starting the application context is enough, because Flyway
 * migrates on startup.
 */
class InitialiseDatabase : IntegrationTestBase() {

  @Test
  fun `initialises database`() {
    println("Database has been initialised by IntegrationTestBase")
  }
}
