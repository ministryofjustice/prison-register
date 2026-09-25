package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.model.Agency
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.Court
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Hospital
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuite
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOffice
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository

class PublicApiResourceIntTest : IntegrationTestBase() {

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
  lateinit var prisonRepository: PrisonRepository

  @Autowired
  lateinit var agencyRepository: AgencyRepository

  @MockitoBean
  private lateinit var telemetryClient: TelemetryClient

  @DisplayName("Get all agencies")
  @Nested
  inner class GetAll {
    lateinit var court: Court
    lateinit var hospital: Hospital
    lateinit var policeCustodySuite: PoliceCustodySuite
    lateinit var probationOffice: ProbationOffice
    lateinit var prison: Prison
    lateinit var agency: Agency

    @BeforeEach
    fun setUp() {
      court = dsl.court(
        courtId = "DCOURT",
        name = "Doncaster Court",
        description = "Doncaster Central Court",
        active = true,
      ) {}

      hospital = dsl.hospital(
        hospitalId = "CHOSP",
        name = "Central Hospital",
        description = "Central Secure Hospital",
        active = true,
        highSecurity = true,
      ) {}

      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "BPOLIC",
        name = "Bawtry Police Custody Suite",
        description = "Bawtry Police Custody Suite",
        active = false,
      ) {}

      probationOffice = dsl.probationOffice(
        probationOfficeId = "APROB",
        name = "Aire Probation Office",
        description = "Aire Valley Probation Office",
        active = true,
      ) {}

      prison = dsl.prison(
        prisonId = "FPRIS",
        name = "Fictional Prison",
        description = "Fictional Prison Description",
        active = true,
      ) {}

      agency = agencyRepository.save(
        Agency(
          agencyId = "EAGEN",
          name = "Example Agency",
          description = "Example Agency Description",
          active = true,
          accessibleAccess = null,
          agencyType = AgencyType.AIRPORT,
          inactiveDate = null,
          cjitCode = null,
          area = null,
          region = null,
          geographicalArea = null,
          payrollRegion = null,
          localAuthority = null,
        ),
      )
    }

    @AfterEach
    fun tearDown() {
      courtRepository.deleteById(court.courtId)
      hospitalRepository.deleteById(hospital.hospitalId)
      policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      probationOfficeRepository.deleteById(probationOffice.probationOfficeId)
      prisonRepository.deleteById(prison.prisonId)
      agencyRepository.deleteById(agency.agencyId)
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/api")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/api")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/api")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return all agencies, ordered by agency ID ascending`() {
        val agencies = webTestClient.get()
          .uri("/api")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
          .exchange()
          .expectStatus().isOk
          .expectBodyList(AgencySummaryDto::class.java)
          .returnResult()
          .responseBody!!

        assertThat(agencies).extracting("agencyId").contains("DCOURT", "CHOSP", "BPOLIC", "APROB", "FPRIS", "EAGEN")

        val ourAgencies = agencies.filter { it.agencyId in listOf("DCOURT", "CHOSP", "BPOLIC", "APROB", "FPRIS", "EAGEN") }
        assertThat(ourAgencies).isSortedAccordingTo(compareBy { it.agencyId })

        val courtDto = agencies.first { it.agencyId == "DCOURT" }
        assertThat(courtDto.description).isEqualTo("Doncaster Central Court")
        assertThat(courtDto.agencyType).isEqualTo(LegacyAgencyType.COURT)
        assertThat(courtDto.active).isTrue

        val hospitalDto = agencies.first { it.agencyId == "CHOSP" }
        assertThat(hospitalDto.description).isEqualTo("Central Secure Hospital")
        assertThat(hospitalDto.agencyType).isEqualTo(LegacyAgencyType.SECURE_HOSPITAL)
        assertThat(hospitalDto.active).isTrue

        val policeCustodySuiteDto = agencies.first { it.agencyId == "BPOLIC" }
        assertThat(policeCustodySuiteDto.description).isEqualTo("Bawtry Police Custody Suite")
        assertThat(policeCustodySuiteDto.agencyType).isEqualTo(LegacyAgencyType.POLICE_CUSTODY_SUITE)
        assertThat(policeCustodySuiteDto.active).isFalse

        val probationOfficeDto = agencies.first { it.agencyId == "APROB" }
        assertThat(probationOfficeDto.description).isEqualTo("Aire Valley Probation Office")
        assertThat(probationOfficeDto.agencyType).isEqualTo(LegacyAgencyType.PROBATION_OFFICE)
        assertThat(probationOfficeDto.active).isTrue

        val prisonDto = agencies.first { it.agencyId == "FPRIS" }
        assertThat(prisonDto.description).isEqualTo("Fictional Prison Description")
        assertThat(prisonDto.agencyType).isEqualTo(LegacyAgencyType.PRISON)
        assertThat(prisonDto.active).isTrue

        val agencyDto = agencies.first { it.agencyId == "EAGEN" }
        assertThat(agencyDto.description).isEqualTo("Example Agency Description")
        assertThat(agencyDto.agencyType).isEqualTo(LegacyAgencyType.AIRPORT)
        assertThat(agencyDto.active).isTrue
      }
    }
  }
}
