package uk.gov.justice.digital.hmpps.prisonregister.resource

import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.hibernate.stat.Statistics
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository

/**
 * The agency list endpoints used to load each agency's addresses, phone numbers and email addresses
 * one agency at a time, so a list of 1,000 courts made about 3,000 queries (MAPB-1126).
 * These tests check the number of queries does not grow with the number of agencies.
 */
class AgencyListQueryCountIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var courtRepository: CourtRepository

  @Autowired
  lateinit var probationOfficeRepository: ProbationOfficeRepository

  @Autowired
  lateinit var entityManagerFactory: EntityManagerFactory

  private val statistics: Statistics by lazy { entityManagerFactory.unwrap(SessionFactory::class.java).statistics }

  private val courtIds = (1..AGENCIES).map { "QC%03d".format(it) }
  private val probationOfficeIds = (1..AGENCIES).map { "QP%03d".format(it) }

  @BeforeEach
  fun setUp() {
    courtIds.forEach { id ->
      dsl.court(courtId = id, name = "Query count court $id") {
        address(addressLine1 = "1 High Street", town = "Sheffield", postcode = "S1 1AA")
        email(emailAddress = "$id@justice.gov.uk")
        phoneNumber(phoneNumber = "0114 555 0000")
      }
    }
    probationOfficeIds.forEach { id ->
      dsl.probationOffice(probationOfficeId = id, name = "Query count probation office $id") {
        address(addressLine1 = "2 High Street", town = "Leeds", postcode = "LS1 1AA")
        email(emailAddress = "$id@justice.gov.uk")
        phoneNumber(phoneNumber = "0113 555 0000")
      }
    }
  }

  @AfterEach
  fun tearDown() {
    courtRepository.deleteAllById(courtIds)
    probationOfficeRepository.deleteAllById(probationOfficeIds)
  }

  @ParameterizedTest(name = "{0} makes a fixed number of queries")
  @CsvSource(
    "/courts, ROLE_HMPPS_REGISTERS_API__MAINTAIN__RW",
    "/probation-offices, ROLE_HMPPS_REGISTERS_API__MAINTAIN__RW",
    "/api/agencies/type/COURT, ROLE_HMPPS_REGISTERS_API__R",
    "/api/agencies/type/COURT?activeOnly=false, ROLE_HMPPS_REGISTERS_API__R",
    "/api/agencies/legacy-type/CRT, ROLE_HMPPS_REGISTERS_API__R",
    "/api/agencies/type/PROBATION_OFFICE, ROLE_HMPPS_REGISTERS_API__R",
  )
  fun `list endpoint makes a fixed number of queries`(path: String, role: String) {
    statistics.clear()

    webTestClient.get().uri(path)
      .headers(setAuthorisation(roles = listOf(role)))
      .exchange()
      .expectStatus().isOk

    // One query per agency for each of addresses, phones and emails would be at least 3 x AGENCIES
    assertThat(statistics.prepareStatementCount).isLessThan(MAX_QUERIES)
  }

  private companion object {
    const val AGENCIES = 20
    const val MAX_QUERIES = 15L
  }
}
