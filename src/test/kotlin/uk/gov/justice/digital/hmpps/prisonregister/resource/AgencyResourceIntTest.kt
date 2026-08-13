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
import uk.gov.justice.digital.hmpps.prisonregister.model.Agency
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyType
import java.time.LocalDate

class AgencyResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var agencyRepository: AgencyRepository

  @DisplayName("Get agency by id")
  @Nested
  inner class GetById {
    lateinit var agency: Agency

    @BeforeEach
    fun setUp() {
      agency = dsl.agency(
        agencyId = "SHEFCC",
        name = "Sheffield Crown Court",
        description = "Sheffield Crown Court City Centre",
        active = false,
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
        agencyType = AgencyType.PROBATION_CRC,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {
        address(
          addressLine1 = "Crown Court, 1 Bank Street",
          addressLine2 = "City Centre",
          town = "Sheffield",
          county = "South Yorkshire",
          postcode = "S1 2DS",
          country = "England",
        )
        address(
          postcode = "S10 2HH",
        )
        email(
          emailAddress = "sheffield@justice.gov.uk",
        )
        email(
          emailAddress = "sheffield2@justice.gov.uk",
        )
        phoneNumber(
          phoneNumber = "0114 555 1234",
        )
        phoneNumber(
          phoneNumber = "0114 555 5678",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      if (::agency.isInitialized) {
        agencyRepository.deleteById(agency.agencyId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/agencies/id/SHEFCC")
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
          .uri("/agencies/id/ZZZZ")
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
        val agencyDto: AgencyDto = webTestClient.get()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agencyDto.agencyId).isEqualTo("SHEFCC")
        assertThat(agencyDto.agencyName).isEqualTo("Sheffield Crown Court")
        assertThat(agencyDto.description).isEqualTo("Sheffield Crown Court City Centre")
        assertThat(agencyDto.active).isFalse
        assertThat(agencyDto.accessibleAccess).isEqualTo("ACCESSIBLE")
        assertThat(agencyDto.agencyType).isEqualTo("PROBATION_CRC")
        assertThat(agencyDto.inactiveDate).isEqualTo("2020-01-02")
        assertThat(agencyDto.area?.description).isEqualTo("South Yorkshire")
        assertThat(agencyDto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(agencyDto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(agencyDto.payrollRegion?.description).isEqualTo("North East & Yorkshire")
        assertThat(agencyDto.localAuthority?.description).isEqualTo("Sheffied City Council")
      }

      @Test
      fun `will return addresses`() {
        val agencyDto: AgencyDto = webTestClient.get()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agencyDto.addresses).hasSize(2)
        assertThat(agencyDto.addresses[0].addressLine1).isEqualTo("Crown Court, 1 Bank Street")
        assertThat(agencyDto.addresses[0].addressLine2).isEqualTo("City Centre")
        assertThat(agencyDto.addresses[0].town).isEqualTo("Sheffield")
        assertThat(agencyDto.addresses[0].county).isEqualTo("South Yorkshire")
        assertThat(agencyDto.addresses[0].postcode).isEqualTo("S1 2DS")
        assertThat(agencyDto.addresses[0].country).isEqualTo("England")
      }

      @Test
      fun `will return emails`() {
        val agencyDto: AgencyDto = webTestClient.get()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agencyDto.emailAddresses).hasSize(2)
        assertThat(agencyDto.emailAddresses[0].address).isEqualTo("sheffield@justice.gov.uk")
      }

      @Test
      fun `will return phone numbers`() {
        val agencyDto: AgencyDto = webTestClient.get()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agencyDto.phoneNumbers).hasSize(2)
        assertThat(agencyDto.phoneNumbers[0].number).isEqualTo("0114 555 1234")
      }

      @Test
      fun `will return payroll region`() {
        val agencyDto: AgencyDto = webTestClient.get()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agencyDto.payrollRegion?.code).isEqualTo("NEY")
        assertThat(agencyDto.payrollRegion?.description).isEqualTo("North East & Yorkshire")
      }
    }
  }
}
