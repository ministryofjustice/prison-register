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
import uk.gov.justice.digital.hmpps.prisonregister.model.Court
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import java.time.LocalDate

class CourtResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var courtRepository: CourtRepository

  @DisplayName("Get court by id")
  @Nested
  inner class GetById {
    lateinit var court: Court

    @BeforeEach
    fun setUp() {
      court = dsl.court(
        courtId = "SHEFCC",
        name = "Sheffield Central Ct",
        description = "Sheffield Central Court",
        active = false,
        inactiveDate = LocalDate.parse("2020-01-02"),
        courtTypeCode = "CC",
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
      ) {
        address(
          addressLine1 = "Court House, 31 High Street",
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
      if (::court.isInitialized) {
        courtRepository.deleteById(court.courtId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/courts/id/SHEFCC")
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
          .uri("/courts/id/ZZZZ")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return core details `() {
        val courtDto: CourtDto = webTestClient.get()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(courtDto.courtId).isEqualTo("SHEFCC")
        assertThat(courtDto.courtName).isEqualTo("Sheffield Central Ct")
        assertThat(courtDto.description).isEqualTo("Sheffield Central Court")
        assertThat(courtDto.active).isFalse
        assertThat(courtDto.inactiveDate).isEqualTo("2020-01-02")
        assertThat(courtDto.courtType?.description).isEqualTo("Crown Court")
        assertThat(courtDto.area?.description).isEqualTo("South Yorkshire")
        assertThat(courtDto.region?.description).isEqualTo("Yorkshire & Humberside")
      }

      @Test
      fun `will return addresses `() {
        val courtDto: CourtDto = webTestClient.get()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(courtDto.addresses).hasSize(2)
        assertThat(courtDto.addresses[0].addressLine1).isEqualTo("Court House, 31 High Street")
        assertThat(courtDto.addresses[0].addressLine2).isEqualTo("City Centre")
        assertThat(courtDto.addresses[0].town).isEqualTo("Sheffield")
        assertThat(courtDto.addresses[0].county).isEqualTo("South Yorkshire")
        assertThat(courtDto.addresses[0].postcode).isEqualTo("S1 3GG")
        assertThat(courtDto.addresses[0].country).isEqualTo("England")
      }

      @Test
      fun `will return emails `() {
        val courtDto: CourtDto = webTestClient.get()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(courtDto.emailAddresses).hasSize(2)
        assertThat(courtDto.emailAddresses[0].address).isEqualTo("test@justice.gov.uk")
      }

      @Test
      fun `will return phone numbers `() {
        val courtDto: CourtDto = webTestClient.get()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(courtDto.phoneNumbers).hasSize(2)
        assertThat(courtDto.phoneNumbers[0].number).isEqualTo("0114 555 8989")
      }
    }
  }
}
