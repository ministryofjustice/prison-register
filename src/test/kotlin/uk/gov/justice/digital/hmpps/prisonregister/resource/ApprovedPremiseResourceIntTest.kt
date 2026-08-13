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
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremise
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremiseRepository
import java.time.LocalDate

class ApprovedPremiseResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var approvedPremiseRepository: ApprovedPremiseRepository

  @DisplayName("Get approved premise by id")
  @Nested
  inner class GetById {
    lateinit var approvedPremise: ApprovedPremise

    @BeforeEach
    fun setUp() {
      approvedPremise = dsl.approvedPremise(
        approvedPremiseId = "SHEFAP",
        name = "Sheffield Approved Premise",
        description = "Sheffield City Centre Approved Premise",
        contact = "John Smith",
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
          addressLine1 = "Approved Premise House, 31 High Street",
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
      if (::approvedPremise.isInitialized) {
        approvedPremiseRepository.deleteById(approvedPremise.approvedPremiseId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/approved-premises/id/SHEFAP")
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
          .uri("/approved-premises/id/ZZZZ")
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
        val dto: ApprovedPremiseDto = webTestClient.get()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(dto.approvedPremiseId).isEqualTo("SHEFAP")
        assertThat(dto.approvedPremiseName).isEqualTo("Sheffield Approved Premise")
        assertThat(dto.description).isEqualTo("Sheffield City Centre Approved Premise")
        assertThat(dto.contact).isEqualTo("John Smith")
        assertThat(dto.active).isFalse
        assertThat(dto.accessibleAccess).isEqualTo("ACCESSIBLE")
        assertThat(dto.inactiveDate).isEqualTo("2020-01-02")
        assertThat(dto.area?.description).isEqualTo("South Yorkshire")
        assertThat(dto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(dto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(dto.localAuthority?.description).isEqualTo("Sheffield City Council")
      }

      @Test
      fun `will return addresses`() {
        val dto: ApprovedPremiseDto = webTestClient.get()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(dto.addresses).hasSize(2)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("Approved Premise House, 31 High Street")
        assertThat(dto.addresses[0].addressLine2).isEqualTo("City Centre")
        assertThat(dto.addresses[0].town).isEqualTo("Sheffield")
        assertThat(dto.addresses[0].county).isEqualTo("South Yorkshire")
        assertThat(dto.addresses[0].postcode).isEqualTo("S1 3GG")
        assertThat(dto.addresses[0].country).isEqualTo("England")
      }

      @Test
      fun `will return emails`() {
        val dto: ApprovedPremiseDto = webTestClient.get()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(dto.emailAddresses).hasSize(2)
        assertThat(dto.emailAddresses[0].address).isEqualTo("test@justice.gov.uk")
      }

      @Test
      fun `will return phone numbers`() {
        val dto: ApprovedPremiseDto = webTestClient.get()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(dto.phoneNumbers).hasSize(2)
        assertThat(dto.phoneNumbers[0].number).isEqualTo("0114 555 8989")
      }
    }
  }
}
