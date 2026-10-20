package uk.gov.justice.digital.hmpps.prisonregister.resource.legacysync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyUsageDto

@DisplayName("GET /legacy/agency/{agencyId}/is-used")
class LegacySyncAgencyIsUsedResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var otherAgencyRepository: OtherAgencyRepository

  @Autowired
  lateinit var courtRepository: CourtRepository

  @Autowired
  lateinit var hospitalRepository: HospitalRepository

  @Autowired
  lateinit var probationOfficeRepository: ProbationOfficeRepository

  @Autowired
  lateinit var approvedPremisesRepository: ApprovedPremisesRepository

  @Autowired
  lateinit var policeCustodySuiteRepository: PoliceCustodySuiteRepository

  @Autowired
  lateinit var dsl: Root

  @AfterEach
  fun tearDown() {
    courtRepository.deleteAll()
    hospitalRepository.deleteAll()
    probationOfficeRepository.deleteAll()
    approvedPremisesRepository.deleteAll()
    policeCustodySuiteRepository.deleteAll()
    otherAgencyRepository.deleteAll()
  }

  @Nested
  inner class Security {
    @Test
    fun `requires a valid authentication token`() {
      webTestClient.get()
        .uri("/legacy/agency/MDI/is-used")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      webTestClient.get()
        .uri("/legacy/agency/MDI/is-used")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `requires correct role for UI this is not used for sync`() {
      webTestClient.get()
        .uri("/legacy/agency/MDI/is-used")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  inner class Validation {
    @Test
    fun `rejects an agency id that is too long`() {
      webTestClient.get()
        .uri("/legacy/agency/TOOLONGID/is-used")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  inner class NotUsed {
    @Test
    fun `returns 204 with no body when the agency id is not used`() {
      webTestClient.get()
        .uri("/legacy/agency/NOTUSD/is-used")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
        .exchange()
        .expectStatus().isNoContent
        .expectBody().isEmpty
    }
  }

  @Nested
  inner class Used {
    private fun isUsed(agencyId: String): LegacyAgencyUsageDto = webTestClient.get()
      .uri("/legacy/agency/$agencyId/is-used")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
      .exchange()
      .expectStatus().isOk
      .expectBodyResponse()

    @Test
    fun `returns prison`() {
      val response = isUsed("MDI")
      assertThat(response.agencyId).isEqualTo("MDI")
      assertThat(response.name).isNotBlank()
      assertThat(response.legacyType).isEqualTo(LegacyAgencyType.PRISON)
    }

    @Test
    fun `returns court`() {
      dsl.court(courtId = "USECRT", name = "Used Court") { }
      assertThat(isUsed("USECRT")).isEqualTo(LegacyAgencyUsageDto("USECRT", "Used Court", LegacyAgencyType.COURT))
    }

    @Test
    fun `returns hospital`() {
      dsl.hospital(hospitalId = "USEHSP", name = "Used Hospital", highSecurity = false) { }
      assertThat(isUsed("USEHSP")).isEqualTo(LegacyAgencyUsageDto("USEHSP", "Used Hospital", LegacyAgencyType.HOSPITAL))
    }

    @Test
    fun `returns secure hospital`() {
      dsl.hospital(hospitalId = "USESHP", name = "Used Secure Hospital", highSecurity = true) { }
      assertThat(isUsed("USESHP")).isEqualTo(LegacyAgencyUsageDto("USESHP", "Used Secure Hospital", LegacyAgencyType.SECURE_HOSPITAL))
    }

    @Test
    fun `returns probation office`() {
      dsl.probationOffice(probationOfficeId = "USEPBO", name = "Used Probation Office") { }
      assertThat(isUsed("USEPBO")).isEqualTo(LegacyAgencyUsageDto("USEPBO", "Used Probation Office", LegacyAgencyType.PROBATION_OFFICE))
    }

    @Test
    fun `returns approved premises`() {
      dsl.approvedPremises(approvedPremisesId = "USEAPR", name = "Used Approved Premises") { }
      assertThat(isUsed("USEAPR")).isEqualTo(LegacyAgencyUsageDto("USEAPR", "Used Approved Premises", LegacyAgencyType.APPROVED_PREMISES))
    }

    @Test
    fun `returns police custody suite`() {
      dsl.policeCustodySuite(policeCustodySuiteId = "USEPCS", name = "Used Police Custody Suite") { }
      assertThat(isUsed("USEPCS")).isEqualTo(LegacyAgencyUsageDto("USEPCS", "Used Police Custody Suite", LegacyAgencyType.POLICE_CUSTODY_SUITE))
    }

    @Test
    fun `returns generic agency with its agency type`() {
      dsl.agency(agencyId = "USEAIR", name = "Used Airport", otherAgencyType = OtherAgencyType.AIRPORT) { }
      assertThat(isUsed("USEAIR")).isEqualTo(LegacyAgencyUsageDto("USEAIR", "Used Airport", LegacyAgencyType.AIRPORT))
    }

    @Test
    fun `maps every generic agency type to a legacy agency type`() {
      OtherAgencyType.entries.forEachIndexed { index, type ->
        val agencyId = "USE%03d".format(index)
        dsl.agency(agencyId = agencyId, name = "Agency $type", otherAgencyType = type) { }
        assertThat(isUsed(agencyId).legacyType.name).isEqualTo(type.name)
      }
    }
  }
}
