package uk.gov.justice.digital.hmpps.prisonregister.resource

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
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOffice
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import java.time.LocalDate

class ProbationOfficeResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var probationOfficeRepository: ProbationOfficeRepository

  @DisplayName("Get probation office by id")
  @Nested
  inner class GetById {
    lateinit var probationOffice: ProbationOffice

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
        active = false,
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
      ) {
        address(
          addressLine1 = "Probation House, 31 High Street",
          addressLine2 = "City Centre",
          town = "Sheffield",
          county = "South Yorkshire",
          postcode = "S1 3GG",
          country = "England",
        )
        address(
          postcode = "S10 2HH",
        )
        email(
          emailAddress = "test@justice.gov.uk",
        )
        email(
          emailAddress = "another@justice.gov.uk",
        )
        phoneNumber(
          phoneNumber = "0114 555 8989",
        )
        phoneNumber(
          phoneNumber = "0114 555 5555",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      if (::probationOffice.isInitialized) {
        probationOfficeRepository.deleteById(probationOffice.probationOfficeId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if not found`() {
        webTestClient.get()
          .uri("/probation-offices/id/ZZZZ")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return core details`() {
        val probationOfficeDto: ProbationOfficeDto = webTestClient.get()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(probationOfficeDto.probationOfficeId).isEqualTo("SHEFPB")
        assertThat(probationOfficeDto.probationOfficeName).isEqualTo("Sheffield Probation Office")
        assertThat(probationOfficeDto.description).isEqualTo("Sheffield City Centre Probation Office")
        assertThat(probationOfficeDto.active).isFalse
        assertThat(probationOfficeDto.accessibleAccess).isEqualTo("ACCESSIBLE")
        assertThat(probationOfficeDto.inactiveDate).isEqualTo("2020-01-02")
        assertThat(probationOfficeDto.area?.description).isEqualTo("South Yorkshire")
        assertThat(probationOfficeDto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(probationOfficeDto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(probationOfficeDto.localAuthority?.description).isEqualTo("Sheffied City Council")
      }

      @Test
      fun `will return addresses`() {
        val probationOfficeDto: ProbationOfficeDto = webTestClient.get()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(probationOfficeDto.addresses).hasSize(2)
        assertThat(probationOfficeDto.addresses[0].addressLine1).isEqualTo("Probation House, 31 High Street")
        assertThat(probationOfficeDto.addresses[0].addressLine2).isEqualTo("City Centre")
        assertThat(probationOfficeDto.addresses[0].town).isEqualTo("Sheffield")
        assertThat(probationOfficeDto.addresses[0].county).isEqualTo("South Yorkshire")
        assertThat(probationOfficeDto.addresses[0].postcode).isEqualTo("S1 3GG")
        assertThat(probationOfficeDto.addresses[0].country).isEqualTo("England")
      }

      @Test
      fun `will return emails`() {
        val probationOfficeDto: ProbationOfficeDto = webTestClient.get()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(probationOfficeDto.emailAddresses).hasSize(2)
        assertThat(probationOfficeDto.emailAddresses[0].address).isEqualTo("test@justice.gov.uk")
      }

      @Test
      fun `will return phone numbers`() {
        val probationOfficeDto: ProbationOfficeDto = webTestClient.get()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(probationOfficeDto.phoneNumbers).hasSize(2)
        assertThat(probationOfficeDto.phoneNumbers[0].number).isEqualTo("0114 555 8989")
      }
    }
  }
}
