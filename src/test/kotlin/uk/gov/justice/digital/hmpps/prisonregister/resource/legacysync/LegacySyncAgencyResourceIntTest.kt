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
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType

class LegacySyncAgencyResourceIntTest : IntegrationTestBase() {

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
  @DisplayName("GET /legacy/{agencyId}")
  inner class GetAgencyDetails {

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/legacy/SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/legacy/SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns details for a court, including database ids`() {
        dsl.court(
          courtId = "SHEFMC",
          name = "Sheffield MC",
          description = "Sheffield Magistrates' Court",
        ) {
          address(addressLine1 = "Castle Street", town = "Sheffield", county = "South Yorkshire", postcode = "S3 8LU", country = "England")
          email(emailAddress = "test.sheffield.mc@justice.gov.uk")
          phoneNumber(phoneNumber = "0114 555 5555")
        }

        val response: LegacyAgencyDto = webTestClient.get()
          .uri("/legacy/SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyType).isEqualTo(LegacyAgencyType.COURT)
        assertThat(response.name).isEqualTo("Sheffield MC")
        assertThat(response.description).isEqualTo("Sheffield Magistrates' Court")

        assertThat(response.addresses).hasSize(1)
        with(response.addresses[0]) {
          assertThat(id).isNotNull
          assertThat(addressLine1).isEqualTo("Castle Street")
          assertThat(town).isEqualTo("Sheffield")
          assertThat(county).isEqualTo("South Yorkshire")
          assertThat(postcode).isEqualTo("S3 8LU")
          assertThat(country).isEqualTo("England")
        }

        assertThat(response.emailAddresses).hasSize(1)
        with(response.emailAddresses[0]) {
          assertThat(id).isNotNull
          assertThat(address).isEqualTo("test.sheffield.mc@justice.gov.uk")
        }

        assertThat(response.phoneNumbers).hasSize(1)
        with(response.phoneNumbers[0]) {
          assertThat(id).isNotNull
          assertThat(number).isEqualTo("0114 555 5555")
        }
      }

      @Test
      fun `returns details for a generic agency`() {
        dsl.agency(
          agencyId = "SFCRC",
          name = "Sheffield CRC",
        ) {
          address(addressLine1 = "1 Charter Row", town = "Sheffield", county = "South Yorkshire", postcode = "S1 3FZ", country = "England")
          email(emailAddress = "sheffield.crc@justice.gov.uk")
          phoneNumber(phoneNumber = "0114 555 1234")
        }

        val response: LegacyAgencyDto = webTestClient.get()
          .uri("/legacy/SFCRC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.name).isEqualTo("Sheffield CRC")
        assertThat(response.addresses).hasSize(1)
        assertThat(response.addresses[0].id).isNotNull
        assertThat(response.emailAddresses).hasSize(1)
        assertThat(response.emailAddresses[0].id).isNotNull
        assertThat(response.phoneNumbers).hasSize(1)
        assertThat(response.phoneNumbers[0].id).isNotNull
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 404 when agency is not found`() {
        webTestClient.get()
          .uri("/legacy/NOTFND")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }
}
