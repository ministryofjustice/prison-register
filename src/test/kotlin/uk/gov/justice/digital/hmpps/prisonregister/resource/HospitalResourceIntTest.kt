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
import uk.gov.justice.digital.hmpps.prisonregister.model.Hospital
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import java.time.LocalDate

class HospitalResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var hospitalRepository: HospitalRepository

  @DisplayName("Get hospital by id")
  @Nested
  inner class GetById {
    lateinit var hospital: Hospital

    @BeforeEach
    fun setUp() {
      hospital = dsl.hospital(
        hospitalId = "SHFHOS",
        name = "Sheffield Secure Hospital",
        description = "Sheffield Central Secure Hospital",
        active = false,
        highSecurity = true,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        geographicalAreaCode = "WYORKS",
        regionCode = "YOHUM",
        payrollRegionCode = "HS",
      ) {
        address(
          addressLine1 = "Hospital House, 31 High Street",
          addressLine2 = "City Centre",
          town = "Sheffield",
          county = "South Yorkshire",
          postcode = "S1 3GG",
          country = "England",
        )
        address(
          postcode = "S10 2HH",
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
      if (::hospital.isInitialized) {
        hospitalRepository.deleteById(hospital.hospitalId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/hospitals/id/SHFHOS")
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
          .uri("/hospitals/id/ZZZZ")
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
        val hospitalDto: HospitalDto = webTestClient.get()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(hospitalDto.hospitalId).isEqualTo("SHFHOS")
        assertThat(hospitalDto.hospitalName).isEqualTo("Sheffield Secure Hospital")
        assertThat(hospitalDto.description).isEqualTo("Sheffield Central Secure Hospital")
        assertThat(hospitalDto.active).isFalse
        assertThat(hospitalDto.highSecurity).isTrue
        assertThat(hospitalDto.inactiveDate).isEqualTo("2020-01-02")
        assertThat(hospitalDto.area?.description).isEqualTo("South Yorkshire")
        assertThat(hospitalDto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(hospitalDto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(hospitalDto.payrollRegion?.description).isEqualTo("High Security")
      }

      @Test
      fun `will return addresses `() {
        val hospitalDto: HospitalDto = webTestClient.get()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(hospitalDto.addresses).hasSize(2)
        assertThat(hospitalDto.addresses[0].addressLine1).isEqualTo("Hospital House, 31 High Street")
        assertThat(hospitalDto.addresses[0].addressLine2).isEqualTo("City Centre")
        assertThat(hospitalDto.addresses[0].town).isEqualTo("Sheffield")
        assertThat(hospitalDto.addresses[0].county).isEqualTo("South Yorkshire")
        assertThat(hospitalDto.addresses[0].postcode).isEqualTo("S1 3GG")
        assertThat(hospitalDto.addresses[0].country).isEqualTo("England")
      }

      @Test
      fun `will return phone numbers `() {
        val hospitalDto: HospitalDto = webTestClient.get()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(hospitalDto.phoneNumbers).hasSize(2)
        assertThat(hospitalDto.phoneNumbers[0].number).isEqualTo("0114 555 8989")
      }
    }
  }
}
