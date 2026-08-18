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
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremiseRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository

class LegacySyncAgencyDeleteResourceIntTest : IntegrationTestBase() {
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
  lateinit var prisonRepository: PrisonRepository

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
  @DisplayName("DELETE /legacy/admin/sync/agency/all")
  inner class DeleteAllAgencies {
    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/legacy/admin/sync/agency/all")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/legacy/admin/sync/agency/all")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.delete()
          .uri("/legacy/admin/sync/agency/all")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @BeforeEach
      fun setUp() {
        dsl.court(
          courtId = "DELCRT",
          name = "Court One",
          areaCode = "52",
          regionCode = "YOHUM",
        ) { }
        dsl.hospital(
          hospitalId = "DELHSP",
          name = "Hospital One",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = "HS",
          localAuthorityCode = "00CG",
          highSecurity = false,
        ) { }
        dsl.probationOffice(
          probationOfficeId = "DELPBO",
          name = "Probation Office One",
          areaCode = "52",
          subareaCode = "SHEFF",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          localAuthorityCode = "00CG",
        ) { }
        dsl.approvedPremise(
          approvedPremiseId = "DELAPR",
          name = "Approved Premise One",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          localAuthorityCode = "00CG",
        ) { }
        dsl.policeCustodySuite(
          policeCustodySuiteId = "DELPCS",
          name = "Police Custody Suite One",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          localAuthorityCode = "00CG",
        ) { }
        dsl.agency(
          agencyId = "DELAGY",
          name = "Generic Agency One",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = "HS",
          localAuthorityCode = "00CG",
        ) { }
        prisonRepository.saveAndFlush(Prison("DELPRI", "Delete Test Prison", active = true))
      }

      @Test
      fun `deletes all agency types except prisons`() {
        assertThat(courtRepository.existsById("DELCRT")).isTrue
        assertThat(hospitalRepository.existsById("DELHSP")).isTrue
        assertThat(probationOfficeRepository.existsById("DELPBO")).isTrue
        assertThat(approvedPremiseRepository.existsById("DELAPR")).isTrue
        assertThat(policeCustodySuiteRepository.existsById("DELPCS")).isTrue
        assertThat(agencyRepository.existsById("DELAGY")).isTrue
        assertThat(prisonRepository.existsById("DELPRI")).isTrue

        webTestClient.delete()
          .uri("/legacy/admin/sync/agency/all")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        assertThat(courtRepository.existsById("DELCRT")).isFalse
        assertThat(hospitalRepository.existsById("DELHSP")).isFalse
        assertThat(probationOfficeRepository.existsById("DELPBO")).isFalse
        assertThat(approvedPremiseRepository.existsById("DELAPR")).isFalse
        assertThat(policeCustodySuiteRepository.existsById("DELPCS")).isFalse
        assertThat(agencyRepository.existsById("DELAGY")).isFalse
        assertThat(prisonRepository.existsById("DELPRI")).isTrue
      }
    }
  }
}
