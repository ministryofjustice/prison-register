package uk.gov.justice.digital.hmpps.prisonregister.resource.legacysync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremiseRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.AgencyIdsResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType

class LegacySyncAgencyReconciliationResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var agencyRepository: AgencyRepository

  @Autowired
  lateinit var courtRepository: CourtRepository

  @Autowired
  lateinit var hospitalRepository: HospitalRepository

  @Autowired
  lateinit var probationOfficeRepository: ProbationOfficeRepository

  @Autowired
  lateinit var approvedPremiseRepository: ApprovedPremiseRepository

  @Autowired
  lateinit var policeCustodySuiteRepository: PoliceCustodySuiteRepository

  @Autowired
  lateinit var dsl: Root

  @AfterEach
  fun tearDown() {
    courtRepository.deleteAll()
    hospitalRepository.deleteAll()
    probationOfficeRepository.deleteAll()
    approvedPremiseRepository.deleteAll()
    policeCustodySuiteRepository.deleteAll()
    agencyRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /legacy/reconciliation/ids/all")
  inner class GetAllAgencyIds {

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/legacy/reconciliation/ids/all")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/legacy/reconciliation/ids/all")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/legacy/reconciliation/ids/all")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      @BeforeEach
      fun setUp() {
        dsl.court(courtId = "RECCRT", name = "Reconciliation Court") { }
        dsl.hospital(hospitalId = "RECHSP", name = "Reconciliation Hospital", highSecurity = false) { }
        dsl.probationOffice(probationOfficeId = "RECPBO", name = "Reconciliation Probation Office") { }
        dsl.approvedPremise(approvedPremiseId = "RECAPR", name = "Reconciliation Approved Premise") { }
        dsl.policeCustodySuite(policeCustodySuiteId = "RECPCS", name = "Reconciliation Police Custody Suite") { }
        dsl.agency(agencyId = "RECAGY", name = "Reconciliation Agency") { }
      }

      @Test
      fun `returns IDs of all non-prison agency types`() {
        val response: AgencyIdsResponse = webTestClient.get()
          .uri("/legacy/reconciliation/ids/all")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        val ids = response.agencyIds.map { it.agencyId }
        assertThat(ids).contains("RECCRT", "RECHSP", "RECPBO", "RECAPR", "RECPCS", "RECAGY")
      }

      @Test
      fun `returns an empty list when no agencies exist`() {
        courtRepository.deleteAll()
        hospitalRepository.deleteAll()
        probationOfficeRepository.deleteAll()
        approvedPremiseRepository.deleteAll()
        policeCustodySuiteRepository.deleteAll()
        agencyRepository.deleteAll()

        val response: AgencyIdsResponse = webTestClient.get()
          .uri("/legacy/reconciliation/ids/all")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyIds).isEmpty()
      }
    }
  }

  @Nested
  @DisplayName("GET /legacy/reconciliation/{agencyId}")
  inner class GetAgencyDetails {

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/legacy/reconciliation/RECDET")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/legacy/reconciliation/RECDET")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns details for a court`() {
        dsl.court(courtId = "RECDET", name = "Reconciliation Detail Court") { }

        val response: LegacyAgencyDto = webTestClient.get()
          .uri("/legacy/reconciliation/RECDET")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyType).isEqualTo(LegacyAgencyType.COURT)
        assertThat(response.name).isEqualTo("Reconciliation Detail Court")
      }

      @Test
      fun `returns details for a hospital`() {
        dsl.hospital(hospitalId = "RECHSD", name = "Reconciliation Detail Hospital", highSecurity = false) { }

        val response: LegacyAgencyDto = webTestClient.get()
          .uri("/legacy/reconciliation/RECHSD")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyType).isEqualTo(LegacyAgencyType.HOSPITAL)
        assertThat(response.name).isEqualTo("Reconciliation Detail Hospital")
      }

      @Test
      fun `returns SECURE_HOSPITAL type for a high-security hospital`() {
        dsl.hospital(hospitalId = "RECSHD", name = "Reconciliation Detail Secure Hospital", highSecurity = true) { }

        val response: LegacyAgencyDto = webTestClient.get()
          .uri("/legacy/reconciliation/RECSHD")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyType).isEqualTo(LegacyAgencyType.SECURE_HOSPITAL)
        assertThat(response.name).isEqualTo("Reconciliation Detail Secure Hospital")
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 404 when agency is not found`() {
        webTestClient.get()
          .uri("/legacy/reconciliation/NOTFND")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }
}
