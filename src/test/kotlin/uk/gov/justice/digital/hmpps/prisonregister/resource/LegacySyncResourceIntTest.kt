package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository

class LegacySyncResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var courtRepository: CourtRepository

  @Autowired
  lateinit var dsl: Root

  companion object {
    fun legacyAgencyDto() = LegacyAgencyDto(
      agencyType = LegacyAgencyType.COURT,
      name = "Sheffield MC",
      description = "Sheffield Magistrates' Court",
      active = true,
      inactiveDate = null,
      cjitCode = "123456789",
      areaCode = "52",
      regionCode = "YOHUM",
      geographicalAreaCode = null,
      payrollRegionCode = "HS",
      courtTypeCode = "MC",
      addresses = listOf(),
      emailAddresses = listOf(),
      phoneNumbers = listOf(),
    )
  }

  @AfterEach
  fun tearDown() {
    courtRepository.deleteAll()
  }

  @Nested
  inner class Security {
    @Test
    fun `requires a valid authentication token`() {
      webTestClient.post()
        .uri("/sync/agency/id/{agencyId}", "SHEFMC")
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(legacyAgencyDto())
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      webTestClient.post()
        .uri("/sync/agency/id/{agencyId}", "SHEFMC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("BANANAS")))
        .bodyValue(legacyAgencyDto())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `allowed with correct role`() {
      webTestClient.post()
        .uri("/sync/agency/id/{agencyId}", "SHEFMC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
        .bodyValue(legacyAgencyDto())
        .exchange()
        .expectStatus().isOk
    }
  }

  @Nested
  inner class Court {
    val courtRequest = LegacyAgencyDto(
      agencyType = LegacyAgencyType.COURT,
      name = "Sheffield MC",
      description = "Sheffield Magistrates' Court",
      active = true,
      inactiveDate = null,
      cjitCode = "123456789",
      areaCode = "52",
      regionCode = "YOHUM",
      geographicalAreaCode = null,
      payrollRegionCode = null,
      courtTypeCode = "MC",
      addresses = listOf(
        LegacyAgencyAddressDto(
          addressLine1 = "Castle Street",
          addressLine2 = null,
          town = "Sheffield",
          county = "South Yorkshire",
          postcode = "S3 8LU",
          country = "England",
        ),
      ),
      emailAddresses = listOf(LegacyAgencyEmailDto(address = "test.sheffield.mc@justice.gov.uk")),
      phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 5555")),
    )

    @Nested
    inner class Validation {
      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/sync/agency/id/{agencyId}", "SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(courtRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency SHEFMC")
      }

      @Test
      fun `region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/sync/agency/id/{agencyId}", "SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(courtRequest.copy(regionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency SHEFMC")
      }

      @Test
      fun `court type code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/sync/agency/id/{agencyId}", "SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(courtRequest.copy(courtTypeCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ court type not found for agency SHEFMC")
      }

      @Test
      fun `court type code missing`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/sync/agency/id/{agencyId}", "SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(courtRequest.copy(courtTypeCode = null))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("null court type not found for agency SHEFMC")
      }
    }

    @Nested
    inner class NewCourtHappyPath {
      val courtRequest = LegacyAgencyDto(
        agencyType = LegacyAgencyType.COURT,
        name = "Sheffield MC",
        description = "Sheffield Magistrates' Court",
        active = true,
        inactiveDate = null,
        cjitCode = "123456789",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = null,
        payrollRegionCode = null,
        courtTypeCode = "MC",
        addresses = listOf(
          LegacyAgencyAddressDto(
            addressLine1 = "Castle Street",
            addressLine2 = null,
            town = "Sheffield",
            county = "South Yorkshire",
            postcode = "S3 8LU",
            country = "England",
          ),
        ),
        emailAddresses = listOf(LegacyAgencyEmailDto(address = "test.sheffield.mc@justice.gov.uk")),
        phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 5555")),
      )

      @Test
      fun `will create the core court data`() {
        val response: LegacyAgencyResponse = webTestClient.post()
          .uri("/sync/agency/id/{agencyId}", "SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(courtRequest.copy(addresses = emptyList(), emailAddresses = emptyList(), phoneNumbers = emptyList()))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(response.updated).isFalse

        val court = courtRepository.findByCourtId("SHEFMC")

        with(court) {
          assertThat(name).isEqualTo("Sheffield MC")
          assertThat(description).isEqualTo("Sheffield Magistrates' Court")
          assertThat(active).isTrue
          assertThat(inactiveDate).isNull()
          assertThat(cjitCode).isEqualTo("123456789")
          assertThat(area?.description).isEqualTo("South Yorkshire")
          assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
          assertThat(courtType.description).isEqualTo("Magistrates Court")
        }
      }
    }
  }
}
