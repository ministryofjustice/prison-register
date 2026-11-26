package uk.gov.justice.digital.hmpps.prisonregister.resource

import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.hibernate.stat.Statistics
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository

/**
 * GET /api/agencies/{agencyId} used to try each agency table in turn, so a prison took 6 queries and
 * a youth offending team 12 (MAPB-1127). It now finds the agency's type first and loads it from that
 * table only.
 */
class SingleAgencyQueryCountIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var courtRepository: CourtRepository

  @Autowired
  lateinit var hospitalRepository: HospitalRepository

  @Autowired
  lateinit var policeCustodySuiteRepository: PoliceCustodySuiteRepository

  @Autowired
  lateinit var probationOfficeRepository: ProbationOfficeRepository

  @Autowired
  lateinit var approvedPremisesRepository: ApprovedPremisesRepository

  @Autowired
  lateinit var prisonRepository: PrisonRepository

  @Autowired
  lateinit var otherAgencyRepository: OtherAgencyRepository

  @Autowired
  lateinit var entityManagerFactory: EntityManagerFactory

  private val statistics: Statistics by lazy { entityManagerFactory.unwrap(SessionFactory::class.java).statistics }

  @BeforeEach
  fun setUp() {
    dsl.court(courtId = "SQCRT", name = "Single query court", areaCode = AREA, regionCode = REGION) {
      address(addressLine1 = "1 High Street", town = "Sheffield", postcode = "S1 1AA")
      address(addressLine1 = "2 High Street", town = "Sheffield", postcode = "S1 1AB")
      email(emailAddress = "court@justice.gov.uk")
      phoneNumber(phoneNumber = "0114 555 0001")
      phoneNumber(phoneNumber = "0114 555 0002")
    }
    dsl.hospital(hospitalId = "SQHOS", name = "Single query hospital", highSecurity = false, areaCode = AREA, regionCode = REGION) {
      address(addressLine1 = "3 High Street", town = "Leeds", postcode = "LS1 1AA")
      phoneNumber(phoneNumber = "0113 555 0001")
    }
    dsl.policeCustodySuite(policeCustodySuiteId = "SQPOL", name = "Single query police custody suite", areaCode = AREA, regionCode = REGION) {
      address(addressLine1 = "4 High Street", town = "York", postcode = "YO1 1AA")
      email(emailAddress = "police@justice.gov.uk")
      phoneNumber(phoneNumber = "01904 555 001")
    }
    dsl.probationOffice(probationOfficeId = "SQPRO", name = "Single query probation office", areaCode = AREA, regionCode = REGION) {
      address(addressLine1 = "5 High Street", town = "Hull", postcode = "HU1 1AA")
      email(emailAddress = "probation@justice.gov.uk")
      phoneNumber(phoneNumber = "01482 555 001")
    }
    dsl.approvedPremises(approvedPremisesId = "SQAPP", name = "Single query approved premises", areaCode = AREA, regionCode = REGION) {
      address(addressLine1 = "6 High Street", town = "Leeds", postcode = "LS2 1AA")
      email(emailAddress = "ap@justice.gov.uk")
      phoneNumber(phoneNumber = "0113 555 0002")
    }
    dsl.agency(agencyId = "SQYOT", name = "Single query youth offending team", otherAgencyType = OtherAgencyType.YOT, areaCode = AREA, regionCode = REGION) {
      address(addressLine1 = "7 High Street", town = "Leeds", postcode = "LS3 1AA")
      email(emailAddress = "yot@justice.gov.uk")
      phoneNumber(phoneNumber = "0113 555 0003")
    }
    dsl.prison(prisonId = "SQPRI", name = "Single query prison") {
      address(addressLine1 = "8 High Street", town = "Leeds", postcode = "LS4 1AA", country = "England")
    }
  }

  @AfterEach
  fun tearDown() {
    courtRepository.deleteById("SQCRT")
    hospitalRepository.deleteById("SQHOS")
    policeCustodySuiteRepository.deleteById("SQPOL")
    probationOfficeRepository.deleteById("SQPRO")
    approvedPremisesRepository.deleteById("SQAPP")
    otherAgencyRepository.deleteById("SQYOT")
    prisonRepository.deleteById("SQPRI")
  }

  @ParameterizedTest(name = "{1} {0} is found with at most {4} queries")
  @CsvSource(
    "SQCRT, COURT, 2, 2, 4",
    "SQHOS, HOSPITAL, 1, 1, 3",
    "SQPOL, POLICE_CUSTODY_SUITE, 1, 1, 4",
    "SQPRO, PROBATION_OFFICE, 1, 1, 4",
    "SQAPP, APPROVED_PREMISES, 1, 1, 4",
    "SQYOT, YOT, 1, 1, 4",
    "SQPRI, PRISON, 1, 0, 2",
  )
  fun `single agency lookup uses a fixed number of queries`(agencyId: String, agencyType: String, addresses: Int, phones: Int, maxQueries: Long) {
    statistics.clear()

    webTestClient.get().uri("/api/agencies/$agencyId")
      .headers(setAuthorisation(roles = listOf("ROLE_HMPPS_REGISTERS_API__R")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.agencyId").isEqualTo(agencyId)
      .jsonPath("$.agencyType").isEqualTo(agencyType)
      .jsonPath("$.addresses.length()").isEqualTo(addresses)
      .jsonPath("$.phones.length()").isEqualTo(phones)

    assertThat(statistics.prepareStatementCount).isLessThanOrEqualTo(maxQueries)
  }

  @Test
  fun `the area and region are returned`() {
    webTestClient.get().uri("/api/agencies/SQCRT")
      .headers(setAuthorisation(roles = listOf("ROLE_HMPPS_REGISTERS_API__R")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.area.code").isEqualTo(AREA)
      .jsonPath("$.region.code").isEqualTo(REGION)
      .jsonPath("$.courtType").isEqualTo("CC")
      .jsonPath("$.emails[0].address").isEqualTo("court@justice.gov.uk")
  }

  @Test
  fun `an unknown agency is not found with one query`() {
    statistics.clear()

    webTestClient.get().uri("/api/agencies/SQNONE")
      .headers(setAuthorisation(roles = listOf("ROLE_HMPPS_REGISTERS_API__R")))
      .exchange()
      .expectStatus().isNotFound

    assertThat(statistics.prepareStatementCount).isEqualTo(1)
  }

  private companion object {
    const val AREA = "YORK"
    const val REGION = "YOHUM"
  }
}
