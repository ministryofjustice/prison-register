package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumberRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuite
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class PoliceCustodySuiteResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var policeCustodySuiteRepository: PoliceCustodySuiteRepository

  @Autowired
  lateinit var agencyAddressRepository: AgencyAddressRepository

  @Autowired
  lateinit var emailAddressRepository: EmailAddressRepository

  @Autowired
  lateinit var phoneNumberRepository: PhoneNumberRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @DisplayName("Get police custody suite by id")
  @Nested
  inner class GetById {
    lateinit var policeCustodySuite: PoliceCustodySuite

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = false,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {
        address(
          addressLine1 = "Custody Suite, 31 High Street",
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
      if (::policeCustodySuite.isInitialized) {
        policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/police-custody-suites/id/SHFPCS")
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
          .uri("/police-custody-suites/id/ZZZZ")
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
        val dto: PoliceCustodySuiteDto = webTestClient.get()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(dto.policeCustodySuiteId).isEqualTo("SHFPCS")
        assertThat(dto.policeCustodySuiteName).isEqualTo("Sheffield Police Custody Suite")
        assertThat(dto.description).isEqualTo("Sheffield City Centre Police Custody Suite")
        assertThat(dto.active).isFalse
        assertThat(dto.inactiveDate).isEqualTo("2020-01-02")
        assertThat(dto.area?.description).isEqualTo("South Yorkshire")
        assertThat(dto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(dto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(dto.payrollRegion?.code).isEqualTo("NEY")
        assertThat(dto.localAuthority?.description).isEqualTo("Sheffield City Council")
      }

      @Test
      fun `will return addresses`() {
        val dto: PoliceCustodySuiteDto = webTestClient.get()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(dto.addresses).hasSize(2)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("Custody Suite, 31 High Street")
        assertThat(dto.addresses[0].addressLine2).isEqualTo("City Centre")
        assertThat(dto.addresses[0].town).isEqualTo("Sheffield")
        assertThat(dto.addresses[0].county).isEqualTo("South Yorkshire")
        assertThat(dto.addresses[0].postcode).isEqualTo("S1 3GG")
        assertThat(dto.addresses[0].country).isEqualTo("England")
      }

      @Test
      fun `will return emails`() {
        val dto: PoliceCustodySuiteDto = webTestClient.get()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(dto.emailAddresses).hasSize(2)
        assertThat(dto.emailAddresses[0].address).isEqualTo("test@justice.gov.uk")
      }

      @Test
      fun `will return phone numbers`() {
        val dto: PoliceCustodySuiteDto = webTestClient.get()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(dto.phoneNumbers).hasSize(2)
        assertThat(dto.phoneNumbers[0].number).isEqualTo("0114 555 8989")
      }
    }
  }

  @DisplayName("Get all police custody suites")
  @Nested
  inner class GetAll {
    lateinit var policeCustodySuite: PoliceCustodySuite
    lateinit var policeCustodySuite2: PoliceCustodySuite
    lateinit var policeCustodySuite3: PoliceCustodySuite

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = false,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {}

      policeCustodySuite2 = dsl.policeCustodySuite(
        policeCustodySuiteId = "LEEDPC",
        name = "Leeds Police Custody Suite",
      ) {}

      policeCustodySuite3 = dsl.policeCustodySuite(
        policeCustodySuiteId = "BIRMPC",
        name = "Birmingham Police Custody Suite",
      ) {}
    }

    @AfterEach
    fun tearDown() {
      policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      policeCustodySuiteRepository.deleteById(policeCustodySuite2.policeCustodySuiteId)
      policeCustodySuiteRepository.deleteById(policeCustodySuite3.policeCustodySuiteId)
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/police-custody-suites")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/police-custody-suites")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/police-custody-suites")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return all police custody suites`() {
        val policeCustodySuites = webTestClient.get()
          .uri("/police-custody-suites")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyList(PoliceCustodySuiteDto::class.java)
          .returnResult()
          .responseBody!!

        assertThat(policeCustodySuites).extracting("policeCustodySuiteId").contains("SHFPCS", "LEEDPC", "BIRMPC")

        val policeCustodySuiteDto = policeCustodySuites.first { it.policeCustodySuiteId == "SHFPCS" }
        assertThat(policeCustodySuiteDto.policeCustodySuiteName).isEqualTo("Sheffield Police Custody Suite")
        assertThat(policeCustodySuiteDto.description).isEqualTo("Sheffield City Centre Police Custody Suite")
        assertThat(policeCustodySuiteDto.active).isFalse

        val policeCustodySuite2Dto = policeCustodySuites.first { it.policeCustodySuiteId == "LEEDPC" }
        assertThat(policeCustodySuite2Dto.policeCustodySuiteName).isEqualTo("Leeds Police Custody Suite")

        val policeCustodySuite3Dto = policeCustodySuites.first { it.policeCustodySuiteId == "BIRMPC" }
        assertThat(policeCustodySuite3Dto.policeCustodySuiteName).isEqualTo("Birmingham Police Custody Suite")
      }
    }
  }

  @DisplayName("Update police custody suite")
  @Nested
  inner class UpdatePoliceCustodySuite {
    lateinit var policeCustodySuite: PoliceCustodySuite

    val updatePoliceCustodySuiteRequest = UpdatePoliceCustodySuiteDto(
      policeCustodySuiteName = "Sheffield Custody Suite",
      description = "Sheffield City Custody Suite",
      active = true,
      inactiveDate = null,
      cjitCode = "123456789",
      areaCode = "52",
      regionCode = "YOHUM",
      geographicalAreaCode = "WYORKS",
      localAuthorityCode = "00CG",
      payrollRegionCode = "NEY",
    )

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = false,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Custody Suite, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
        email(
          emailAddress = "test@justice.gov.uk",
        )
        phoneNumber(
          phoneNumber = "0114 555 8989",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      if (::policeCustodySuite.isInitialized) {
        policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updatePoliceCustodySuiteRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updatePoliceCustodySuiteRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePoliceCustodySuiteRequest)
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if not found`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/ZZZZ")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePoliceCustodySuiteRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePoliceCustodySuiteRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for police custody suite SHFPCS")
      }

      @Test
      fun `region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePoliceCustodySuiteRequest.copy(regionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for police custody suite SHFPCS")
      }

      @Test
      fun `geographical area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePoliceCustodySuiteRequest.copy(geographicalAreaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for police custody suite SHFPCS")
      }

      @Test
      fun `local authority code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePoliceCustodySuiteRequest.copy(localAuthorityCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for police custody suite SHFPCS")
      }

      @Test
      fun `payroll region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePoliceCustodySuiteRequest.copy(payrollRegionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for police custody suite SHFPCS")
      }

      @Test
      fun `police custody suite name is blank`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePoliceCustodySuiteRequest.copy(policeCustodySuiteName = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the core police custody suite data`() {
        val dto: PoliceCustodySuiteDto = webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePoliceCustodySuiteRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(dto.policeCustodySuiteId).isEqualTo("SHFPCS")
        assertThat(dto.policeCustodySuiteName).isEqualTo("Sheffield Custody Suite")
        assertThat(dto.description).isEqualTo("Sheffield City Custody Suite")
        assertThat(dto.active).isFalse
        assertThat(dto.inactiveDate).isEqualTo("2026-01-01")
        assertThat(dto.cjitCode).isEqualTo("123456789")
        assertThat(dto.area?.description).isEqualTo("South Yorkshire")
        assertThat(dto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(dto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(dto.localAuthority?.description).isEqualTo("Sheffield City Council")
        assertThat(dto.payrollRegion?.code).isEqualTo("NEY")
      }

      @Test
      fun `will not affect addresses, emails or phone numbers`() {
        val dto: PoliceCustodySuiteDto = webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePoliceCustodySuiteRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(dto.addresses).hasSize(1)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("Custody Suite, 31 High Street")
        assertThat(dto.emailAddresses).hasSize(1)
        assertThat(dto.emailAddresses[0].address).isEqualTo("test@justice.gov.uk")
        assertThat(dto.phoneNumbers).hasSize(1)
        assertThat(dto.phoneNumbers[0].number).isEqualTo("0114 555 8989")
      }
    }
  }

  @DisplayName("Update police custody suite address")
  @Nested
  inner class UpdatePoliceCustodySuiteAddress {
    lateinit var policeCustodySuite: PoliceCustodySuite
    var addressId: Long = -1

    val updateAddressRequest = UpdateAddressDto(
      addressLine1 = "Updated Custody Suite, 31 High Street",
      addressLine2 = "Updated City Centre",
      town = "Updated Sheffield",
      county = "Updated South Yorkshire",
      postcode = "S1 4HH",
      country = "Wales",
    )

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Custody Suite, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
      }
      addressId = policeCustodySuite.addresses[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::policeCustodySuite.isInitialized) {
        policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if police custody suite not found`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/ZZZZ/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if address not found`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/address/{addressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if town is missing`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(mapOf("postcode" to "S1 3GG", "country" to "England"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if postcode is too long`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest.copy(postcode = "TOOLONGPOSTCODE"))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the address and preserve its id`() {
        val addressDto: AgencyAddressDto = webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(addressDto.id).isEqualTo(addressId)
        assertThat(addressDto.addressLine1).isEqualTo("Updated Custody Suite, 31 High Street")
        assertThat(addressDto.addressLine2).isEqualTo("Updated City Centre")
        assertThat(addressDto.town).isEqualTo("Updated Sheffield")
        assertThat(addressDto.county).isEqualTo("Updated South Yorkshire")
        assertThat(addressDto.postcode).isEqualTo("S1 4HH")
        assertThat(addressDto.country).isEqualTo("Wales")
      }
    }
  }

  @DisplayName("Update police custody suite phone number")
  @Nested
  inner class UpdatePoliceCustodySuitePhoneNumber {
    lateinit var policeCustodySuite: PoliceCustodySuite
    var phoneNumberId: Long = -1

    val updatePhoneNumberRequest = UpdatePhoneNumberDto(number = "0114 555 1234")

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        phoneNumber(
          phoneNumber = "0114 555 8989",
        )
      }
      phoneNumberId = policeCustodySuite.phoneNumbers[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::policeCustodySuite.isInitialized) {
        policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if police custody suite not found`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/ZZZZ/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if phone number not found`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/phone-number/{phoneNumberId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if phone number is in an incorrect format`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = "not-a-number"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if phone number is blank`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the phone number and preserve its id`() {
        val phoneDto: AgencyPhoneDto = webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(phoneDto.id).isEqualTo(phoneNumberId)
        assertThat(phoneDto.number).isEqualTo("0114 555 1234")
      }
    }
  }

  @DisplayName("Update police custody suite email address")
  @Nested
  inner class UpdatePoliceCustodySuiteEmailAddress {
    lateinit var policeCustodySuite: PoliceCustodySuite
    var emailAddressId: Long = -1

    val updateEmailAddressRequest = UpdateEmailAddressDto(address = "updated@justice.gov.uk")

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        email(
          emailAddress = "test@justice.gov.uk",
        )
      }
      emailAddressId = policeCustodySuite.emailAddresses[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::policeCustodySuite.isInitialized) {
        policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if police custody suite not found`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/ZZZZ/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if email address not found`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/email-address/{emailAddressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if email address is in an incorrect format`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest.copy(address = "not-an-email"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if email address is blank`() {
        webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest.copy(address = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the email address and preserve its id`() {
        val emailDto: AgencyEmailDto = webTestClient.put()
          .uri("/police-custody-suites/id/SHFPCS/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(emailDto.id).isEqualTo(emailAddressId)
        assertThat(emailDto.address).isEqualTo("updated@justice.gov.uk")
      }
    }
  }

  @DisplayName("Create police custody suite")
  @Nested
  inner class CreatePoliceCustodySuite {
    val createPoliceCustodySuiteRequest = CreatePoliceCustodySuiteDto(
      policeCustodySuiteId = "NEWPCS",
      policeCustodySuiteName = "New Police Custody Suite",
      description = "The New Police Custody Suite",
      active = true,
      inactiveDate = null,
      cjitCode = "123456789",
      areaCode = "52",
      regionCode = "YOHUM",
      geographicalAreaCode = "WYORKS",
      localAuthorityCode = "00CG",
      payrollRegionCode = "NEY",
      addresses = listOf(
        UpdateAddressDto(
          addressLine1 = "Custody Suite, 31 High Street",
          addressLine2 = "City Centre",
          town = "Sheffield",
          county = "South Yorkshire",
          postcode = "S1 3GG",
          country = "England",
        ),
      ),
      emailAddresses = listOf(
        UpdateEmailAddressDto(address = "test@justice.gov.uk"),
      ),
      phoneNumbers = listOf(
        UpdatePhoneNumberDto(number = "0114 555 8989"),
        UpdatePhoneNumberDto(number = "0114 555 7777"),
      ),
    )

    @AfterEach
    fun tearDown() {
      policeCustodySuiteRepository.findByIdOrNull(createPoliceCustodySuiteRequest.policeCustodySuiteId)?.let { policeCustodySuiteRepository.delete(it) }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/police-custody-suites")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createPoliceCustodySuiteRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/police-custody-suites")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createPoliceCustodySuiteRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/police-custody-suites")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPoliceCustodySuiteRequest)
          .exchange()
          .expectStatus().isCreated
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `police custody suite id already exists`() {
        dsl.policeCustodySuite(policeCustodySuiteId = createPoliceCustodySuiteRequest.policeCustodySuiteId, name = "Existing Police Custody Suite") {}

        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/police-custody-suites")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPoliceCustodySuiteRequest)
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("Police custody suite ${createPoliceCustodySuiteRequest.policeCustodySuiteId} already exists")
      }

      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/police-custody-suites")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPoliceCustodySuiteRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for police custody suite ${createPoliceCustodySuiteRequest.policeCustodySuiteId}")
      }

      @Test
      fun `police custody suite name is blank`() {
        webTestClient.post()
          .uri("/police-custody-suites")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPoliceCustodySuiteRequest.copy(policeCustodySuiteName = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will persist the police custody suite, address, email address and phone number`() {
        webTestClient.post()
          .uri("/police-custody-suites")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPoliceCustodySuiteRequest)
          .exchange()
          .expectStatus().isCreated

        transactionHelper.runInTransaction {
          val persisted = policeCustodySuiteRepository.findByIdOrNull(createPoliceCustodySuiteRequest.policeCustodySuiteId)

          assertThat(persisted).isNotNull
          assertThat(persisted!!.name).isEqualTo("New Police Custody Suite")
          assertThat(persisted.description).isEqualTo("The New Police Custody Suite")
          assertThat(persisted.active).isTrue
          assertThat(persisted.cjitCode).isEqualTo("123456789")
          assertThat(persisted.area?.code).isEqualTo("52")
          assertThat(persisted.region?.code).isEqualTo("YOHUM")
          assertThat(persisted.geographicalArea?.code).isEqualTo("WYORKS")
          assertThat(persisted.localAuthority?.code).isEqualTo("00CG")
          assertThat(persisted.payrollRegion?.code).isEqualTo("NEY")

          assertThat(persisted.addresses).hasSize(1)
          assertThat(persisted.addresses[0].addressLine1).isEqualTo("Custody Suite, 31 High Street")
          assertThat(persisted.addresses[0].addressLine2).isEqualTo("City Centre")
          assertThat(persisted.addresses[0].town).isEqualTo("Sheffield")
          assertThat(persisted.addresses[0].county).isEqualTo("South Yorkshire")
          assertThat(persisted.addresses[0].postcode).isEqualTo("S1 3GG")
          assertThat(persisted.addresses[0].country).isEqualTo("England")

          assertThat(persisted.emailAddresses).hasSize(1)
          assertThat(persisted.emailAddresses[0].value).isEqualTo("test@justice.gov.uk")

          assertThat(persisted.phoneNumbers).hasSize(2)
          assertThat(persisted.phoneNumbers.map { it.value }).containsExactlyInAnyOrder("0114 555 8989", "0114 555 7777")
        }
      }
    }
  }

  @DisplayName("Create police custody suite address")
  @Nested
  inner class CreatePoliceCustodySuiteAddress {
    lateinit var policeCustodySuite: PoliceCustodySuite

    val createAddressRequest = UpdateAddressDto(
      addressLine1 = "Custody Suite, 31 High Street",
      addressLine2 = "City Centre",
      town = "Sheffield",
      county = "South Yorkshire",
      postcode = "S1 3GG",
      country = "England",
    )

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Existing Custody Suite",
          town = "Leeds",
          postcode = "LS1 1AA",
          country = "England",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      if (::policeCustodySuite.isInitialized) {
        policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/address")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isCreated
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if police custody suite not found`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/ZZZZ/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if town is missing`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(mapOf("postcode" to "S1 3GG", "country" to "England"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if postcode is too long`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAddressRequest.copy(postcode = "TOOLONGPOSTCODE"))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will persist the new address against the police custody suite`() {
        val addressDto: AgencyAddressDto = webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(addressDto.addressLine1).isEqualTo("Custody Suite, 31 High Street")
        assertThat(addressDto.addressLine2).isEqualTo("City Centre")
        assertThat(addressDto.town).isEqualTo("Sheffield")
        assertThat(addressDto.county).isEqualTo("South Yorkshire")
        assertThat(addressDto.postcode).isEqualTo("S1 3GG")
        assertThat(addressDto.country).isEqualTo("England")
        assertThat(addressDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persisted = policeCustodySuiteRepository.findByIdOrNull("SHFPCS")!!
          assertThat(persisted.addresses).hasSize(2)
          val persistedAddress = persisted.addresses.find { it.id == addressDto.id }
          assertThat(persistedAddress).isNotNull
          assertThat(persistedAddress!!.addressLine1).isEqualTo("Custody Suite, 31 High Street")
        }
      }
    }
  }

  @DisplayName("Create police custody suite phone number")
  @Nested
  inner class CreatePoliceCustodySuitePhoneNumber {
    lateinit var policeCustodySuite: PoliceCustodySuite

    val createPhoneNumberRequest = UpdatePhoneNumberDto(number = "0114 555 8989")

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        phoneNumber(
          phoneNumber = "0114 555 1111",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      if (::policeCustodySuite.isInitialized) {
        policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if police custody suite not found`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/ZZZZ/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if phone number is in an incorrect format`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest.copy(number = "not-a-number"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if phone number is blank`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest.copy(number = ""))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `409 if phone number already exists`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated

        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isEqualTo(409).expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("Phone number ${createPhoneNumberRequest.number} already exists")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will persist the new phone number against the police custody suite`() {
        val phoneDto: AgencyPhoneDto = webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(phoneDto.number).isEqualTo("0114 555 8989")
        assertThat(phoneDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persisted = policeCustodySuiteRepository.findByIdOrNull("SHFPCS")!!
          assertThat(persisted.phoneNumbers).hasSize(2)
          val persistedPhoneNumber = persisted.phoneNumbers.find { it.id == phoneDto.id }
          assertThat(persistedPhoneNumber).isNotNull
          assertThat(persistedPhoneNumber!!.value).isEqualTo("0114 555 8989")
        }
      }
    }
  }

  @DisplayName("Create police custody suite email address")
  @Nested
  inner class CreatePoliceCustodySuiteEmailAddress {
    lateinit var policeCustodySuite: PoliceCustodySuite

    val createEmailAddressRequest = UpdateEmailAddressDto(address = "new@justice.gov.uk")

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        email(
          emailAddress = "existing@test.com",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      if (::policeCustodySuite.isInitialized) {
        policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isCreated
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if police custody suite not found`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/ZZZZ/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if email address is in an incorrect format`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest.copy(address = "not-an-email"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if email address is blank`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest.copy(address = ""))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `409 if email address already exists`() {
        webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isCreated

        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isEqualTo(409).expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("Email address ${createEmailAddressRequest.address} already exists")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will persist the new email address against the police custody suite`() {
        val emailDto: AgencyEmailDto = webTestClient.post()
          .uri("/police-custody-suites/id/SHFPCS/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(emailDto.address).isEqualTo("new@justice.gov.uk")
        assertThat(emailDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persisted = policeCustodySuiteRepository.findByIdOrNull("SHFPCS")!!
          assertThat(persisted.emailAddresses).hasSize(2)
          val persistedEmailAddress = persisted.emailAddresses.find { it.id == emailDto.id }
          assertThat(persistedEmailAddress).isNotNull
          assertThat(persistedEmailAddress!!.value).isEqualTo("new@justice.gov.uk")
        }
      }
    }
  }

  @DisplayName("Delete police custody suite")
  @Nested
  inner class DeletePoliceCustodySuite {
    lateinit var policeCustodySuite: PoliceCustodySuite

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = true,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Custody Suite, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
        email(
          emailAddress = "test@justice.gov.uk",
        )
        phoneNumber(
          phoneNumber = "0114 555 8989",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      if (::policeCustodySuite.isInitialized) {
        policeCustodySuiteRepository.findByIdOrNull(policeCustodySuite.policeCustodySuiteId)?.let { policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId) }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if not found`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/ZZZZ")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the police custody suite, along with its addresses, emails and phone numbers`() {
        val addressId = policeCustodySuite.addresses[0].id
        val emailAddressId = policeCustodySuite.emailAddresses[0].id
        val phoneNumberId = policeCustodySuite.phoneNumbers[0].id

        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(policeCustodySuiteRepository.findByIdOrNull("SHFPCS")).isNull()
          assertThat(agencyAddressRepository.findByIdOrNull(addressId)).isNull()
          assertThat(emailAddressRepository.findByIdOrNull(emailAddressId)).isNull()
          assertThat(phoneNumberRepository.findByIdOrNull(phoneNumberId)).isNull()
        }
      }
    }
  }

  @DisplayName("Delete police custody suite address")
  @Nested
  inner class DeletePoliceCustodySuiteAddress {
    lateinit var policeCustodySuite: PoliceCustodySuite
    var addressId: Long = -1

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Custody Suite, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
      }
      addressId = policeCustodySuite.addresses[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::policeCustodySuite.isInitialized) {
        policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if police custody suite not found`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/ZZZZ/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if address not found`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS/address/{addressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the address`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(agencyAddressRepository.findByIdOrNull(addressId)).isNull()
          assertThat(policeCustodySuiteRepository.findByIdOrNull("SHFPCS")?.addresses).isEmpty()
        }
      }
    }
  }

  @DisplayName("Delete police custody suite phone number")
  @Nested
  inner class DeletePoliceCustodySuitePhoneNumber {
    lateinit var policeCustodySuite: PoliceCustodySuite
    var phoneNumberId: Long = -1

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        phoneNumber(
          phoneNumber = "0114 555 8989",
        )
      }
      phoneNumberId = policeCustodySuite.phoneNumbers[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::policeCustodySuite.isInitialized) {
        policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if police custody suite not found`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/ZZZZ/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if phone number not found`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS/phone-number/{phoneNumberId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the phone number`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(phoneNumberRepository.findByIdOrNull(phoneNumberId)).isNull()
          assertThat(policeCustodySuiteRepository.findByIdOrNull("SHFPCS")?.phoneNumbers).isEmpty()
        }
      }
    }
  }

  @DisplayName("Delete police custody suite email address")
  @Nested
  inner class DeletePoliceCustodySuiteEmailAddress {
    lateinit var policeCustodySuite: PoliceCustodySuite
    var emailAddressId: Long = -1

    @BeforeEach
    fun setUp() {
      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "SHFPCS",
        name = "Sheffield Police Custody Suite",
        description = "Sheffield City Centre Police Custody Suite",
        active = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        email(
          emailAddress = "test@justice.gov.uk",
        )
      }
      emailAddressId = policeCustodySuite.emailAddresses[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::policeCustodySuite.isInitialized) {
        policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if police custody suite not found`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/ZZZZ/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if email address not found`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS/email-address/{emailAddressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the email address`() {
        webTestClient.delete()
          .uri("/police-custody-suites/id/SHFPCS/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(emailAddressRepository.findByIdOrNull(emailAddressId)).isNull()
          assertThat(policeCustodySuiteRepository.findByIdOrNull("SHFPCS")?.emailAddresses).isEmpty()
        }
      }
    }
  }
}
