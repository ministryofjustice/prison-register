package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremises
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumberRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class ApprovedPremisesResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var approvedPremisesRepository: ApprovedPremisesRepository

  @Autowired
  lateinit var agencyAddressRepository: AgencyAddressRepository

  @Autowired
  lateinit var emailAddressRepository: EmailAddressRepository

  @Autowired
  lateinit var phoneNumberRepository: PhoneNumberRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @MockitoBean
  private lateinit var telemetryClient: TelemetryClient

  @DisplayName("Get approved premises by id")
  @Nested
  inner class GetById {
    lateinit var approvedPremises: ApprovedPremises

    @BeforeEach
    fun setUp() {
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
        contact = "John Smith",
        active = false,
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {
        address(
          addressLine1 = "Approved Premises House, 31 High Street",
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
      if (::approvedPremises.isInitialized) {
        approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
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
        val dto: ApprovedPremisesDto = webTestClient.get()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(dto.approvedPremisesId).isEqualTo("SHEFAP")
        assertThat(dto.approvedPremisesName).isEqualTo("Sheffield Approved Premises")
        assertThat(dto.description).isEqualTo("Sheffield City Centre Approved Premises")
        assertThat(dto.contact).isEqualTo("John Smith")
        assertThat(dto.active).isFalse
        assertThat(dto.accessibleAccess).isEqualTo("ACCESSIBLE")
        assertThat(dto.inactiveDate).isEqualTo("2020-01-02")
        assertThat(dto.area?.description).isEqualTo("South Yorkshire")
        assertThat(dto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(dto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(dto.payrollRegion?.code).isEqualTo("NEY")
        assertThat(dto.localAuthority?.description).isEqualTo("Sheffield City Council")
      }

      @Test
      fun `will return addresses`() {
        val dto: ApprovedPremisesDto = webTestClient.get()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(dto.addresses).hasSize(2)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("Approved Premises House, 31 High Street")
        assertThat(dto.addresses[0].addressLine2).isEqualTo("City Centre")
        assertThat(dto.addresses[0].town).isEqualTo("Sheffield")
        assertThat(dto.addresses[0].county).isEqualTo("South Yorkshire")
        assertThat(dto.addresses[0].postcode).isEqualTo("S1 3GG")
        assertThat(dto.addresses[0].country).isEqualTo("England")
      }

      @Test
      fun `will return emails`() {
        val dto: ApprovedPremisesDto = webTestClient.get()
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
        val dto: ApprovedPremisesDto = webTestClient.get()
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

  @DisplayName("Get all approved premises")
  @Nested
  inner class GetAll {
    lateinit var approvedPremises: ApprovedPremises
    lateinit var approvedPremises2: ApprovedPremises
    lateinit var approvedPremises3: ApprovedPremises

    @BeforeEach
    fun setUp() {
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
        active = false,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {}

      approvedPremises2 = dsl.approvedPremises(
        approvedPremisesId = "LEEDAP",
        name = "Leeds Approved Premises",
      ) {}

      approvedPremises3 = dsl.approvedPremises(
        approvedPremisesId = "BIRMAP",
        name = "Birmingham Approved Premises",
      ) {}
    }

    @AfterEach
    fun tearDown() {
      approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
      approvedPremisesRepository.deleteById(approvedPremises2.approvedPremisesId)
      approvedPremisesRepository.deleteById(approvedPremises3.approvedPremisesId)
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return all approved premises`() {
        val approvedPremises = webTestClient.get()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyList(ApprovedPremisesDto::class.java)
          .returnResult()
          .responseBody!!

        assertThat(approvedPremises).extracting("approvedPremisesId").contains("SHEFAP", "LEEDAP", "BIRMAP")

        val approvedPremisesDto = approvedPremises.first { it.approvedPremisesId == "SHEFAP" }
        assertThat(approvedPremisesDto.approvedPremisesName).isEqualTo("Sheffield Approved Premises")
        assertThat(approvedPremisesDto.description).isEqualTo("Sheffield City Centre Approved Premises")
        assertThat(approvedPremisesDto.active).isFalse

        val approvedPremises2Dto = approvedPremises.first { it.approvedPremisesId == "LEEDAP" }
        assertThat(approvedPremises2Dto.approvedPremisesName).isEqualTo("Leeds Approved Premises")

        val approvedPremises3Dto = approvedPremises.first { it.approvedPremisesId == "BIRMAP" }
        assertThat(approvedPremises3Dto.approvedPremisesName).isEqualTo("Birmingham Approved Premises")
      }
    }
  }

  @DisplayName("Update approved premises")
  @Nested
  inner class UpdateApprovedPremises {
    lateinit var approvedPremises: ApprovedPremises

    val updateApprovedPremisesRequest = UpdateApprovedPremisesDto(
      approvedPremisesName = "Sheffield Central Approved Premises",
      description = "Sheffield City Approved Premises",
      contact = "Alex Taylor",
      active = true,
      accessibleAccess = AccessibleAccess.WHEELCHAIR_ACCESS,
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
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
        contact = "John Smith",
        active = false,
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Approved Premises House, 31 High Street",
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
      if (::approvedPremises.isInitialized) {
        approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateApprovedPremisesRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateApprovedPremisesRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateApprovedPremisesRequest)
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if not found`() {
        webTestClient.put()
          .uri("/approved-premises/id/ZZZZ")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateApprovedPremisesRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateApprovedPremisesRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for approved premises SHEFAP")
      }

      @Test
      fun `region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateApprovedPremisesRequest.copy(regionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for approved premises SHEFAP")
      }

      @Test
      fun `geographical area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateApprovedPremisesRequest.copy(geographicalAreaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for approved premises SHEFAP")
      }

      @Test
      fun `local authority code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateApprovedPremisesRequest.copy(localAuthorityCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for approved premises SHEFAP")
      }

      @Test
      fun `payroll region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateApprovedPremisesRequest.copy(payrollRegionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for approved premises SHEFAP")
      }

      @Test
      fun `approved premises name is blank`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateApprovedPremisesRequest.copy(approvedPremisesName = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the core approved premises data`() {
        val dto: ApprovedPremisesDto = webTestClient.put()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateApprovedPremisesRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(dto.approvedPremisesId).isEqualTo("SHEFAP")
        assertThat(dto.approvedPremisesName).isEqualTo("Sheffield Central Approved Premises")
        assertThat(dto.description).isEqualTo("Sheffield City Approved Premises")
        assertThat(dto.contact).isEqualTo("Alex Taylor")
        assertThat(dto.active).isFalse
        assertThat(dto.accessibleAccess).isEqualTo("WHEELCHAIR_ACCESS")
        assertThat(dto.inactiveDate).isEqualTo("2026-01-01")
        assertThat(dto.cjitCode).isEqualTo("123456789")
        assertThat(dto.area?.description).isEqualTo("South Yorkshire")
        assertThat(dto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(dto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(dto.localAuthority?.description).isEqualTo("Sheffield City Council")
        assertThat(dto.payrollRegion?.code).isEqualTo("NEY")

        verify(telemetryClient).trackEvent(
          eq("approved-premises-updated"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
          },
          isNull(),
        )
      }

      @Test
      fun `will not affect addresses, emails or phone numbers`() {
        val dto: ApprovedPremisesDto = webTestClient.put()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateApprovedPremisesRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(dto.addresses).hasSize(1)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("Approved Premises House, 31 High Street")
        assertThat(dto.emailAddresses).hasSize(1)
        assertThat(dto.emailAddresses[0].address).isEqualTo("test@justice.gov.uk")
        assertThat(dto.phoneNumbers).hasSize(1)
        assertThat(dto.phoneNumbers[0].number).isEqualTo("0114 555 8989")

        verify(telemetryClient).trackEvent(
          eq("approved-premises-updated"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Update approved premises address")
  @Nested
  inner class UpdateApprovedPremisesAddress {
    lateinit var approvedPremises: ApprovedPremises
    var addressId: Long = -1

    val updateAddressRequest = UpdateAddressDto(
      addressLine1 = "Updated Approved Premises House, 31 High Street",
      addressLine2 = "Updated City Centre",
      town = "Updated Sheffield",
      county = "Updated South Yorkshire",
      postcode = "S1 4HH",
      country = "Wales",
    )

    @BeforeEach
    fun setUp() {
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
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
          addressLine1 = "Approved Premises House, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
      }
      addressId = approvedPremises.addresses[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::approvedPremises.isInitialized) {
        approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/address/{addressId}", addressId)
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
      fun `404 if approved premises not found`() {
        webTestClient.put()
          .uri("/approved-premises/id/ZZZZ/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if address not found`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/address/{addressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if town is missing`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(mapOf("postcode" to "S1 3GG", "country" to "England"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if postcode is too long`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/address/{addressId}", addressId)
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
          .uri("/approved-premises/id/SHEFAP/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(addressDto.id).isEqualTo(addressId)
        assertThat(addressDto.addressLine1).isEqualTo("Updated Approved Premises House, 31 High Street")
        assertThat(addressDto.addressLine2).isEqualTo("Updated City Centre")
        assertThat(addressDto.town).isEqualTo("Updated Sheffield")
        assertThat(addressDto.county).isEqualTo("Updated South Yorkshire")
        assertThat(addressDto.postcode).isEqualTo("S1 4HH")
        assertThat(addressDto.country).isEqualTo("Wales")

        verify(telemetryClient).trackEvent(
          eq("approved-premises-address-updated"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
            assertThat(it["addressId"]).isEqualTo(addressId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Update approved premises phone number")
  @Nested
  inner class UpdateApprovedPremisesPhoneNumber {
    lateinit var approvedPremises: ApprovedPremises
    var phoneNumberId: Long = -1

    val updatePhoneNumberRequest = UpdatePhoneNumberDto(number = "0114 555 1234")

    @BeforeEach
    fun setUp() {
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
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
        phoneNumber(
          phoneNumber = "0114 555 4321",
        )
      }
      phoneNumberId = approvedPremises.phoneNumbers.first { it.value == "0114 555 8989" }.id
    }

    @AfterEach
    fun tearDown() {
      if (::approvedPremises.isInitialized) {
        approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/phone-number/{phoneNumberId}", phoneNumberId)
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
      fun `404 if approved premises not found`() {
        webTestClient.put()
          .uri("/approved-premises/id/ZZZZ/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if phone number not found`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/phone-number/{phoneNumberId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if phone number is in an incorrect format`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = "not-a-number"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if phone number is blank`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/phone-number/{phoneNumberId}", phoneNumberId)
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
          .uri("/approved-premises/id/SHEFAP/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(phoneDto.id).isEqualTo(phoneNumberId)
        assertThat(phoneDto.number).isEqualTo("0114 555 1234")

        verify(telemetryClient).trackEvent(
          eq("approved-premises-phone-number-updated"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneNumberId.toString())
          },
          isNull(),
        )
      }

      @Test
      fun `will allow updating a phone number to its own current value`() {
        val phoneDto: AgencyPhoneDto = webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = "0114 555 8989"))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(phoneDto.number).isEqualTo("0114 555 8989")

        verify(telemetryClient).trackEvent(
          eq("approved-premises-phone-number-updated"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneNumberId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Update approved premises email address")
  @Nested
  inner class UpdateApprovedPremisesEmailAddress {
    lateinit var approvedPremises: ApprovedPremises
    var emailAddressId: Long = -1

    val updateEmailAddressRequest = UpdateEmailAddressDto(address = "updated@justice.gov.uk")

    @BeforeEach
    fun setUp() {
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
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
      emailAddressId = approvedPremises.emailAddresses[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::approvedPremises.isInitialized) {
        approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/email-address/{emailAddressId}", emailAddressId)
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
      fun `404 if approved premises not found`() {
        webTestClient.put()
          .uri("/approved-premises/id/ZZZZ/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if email address not found`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/email-address/{emailAddressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if email address is in an incorrect format`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest.copy(address = "not-an-email"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if email address is blank`() {
        webTestClient.put()
          .uri("/approved-premises/id/SHEFAP/email-address/{emailAddressId}", emailAddressId)
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
          .uri("/approved-premises/id/SHEFAP/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(emailDto.id).isEqualTo(emailAddressId)
        assertThat(emailDto.address).isEqualTo("updated@justice.gov.uk")

        verify(telemetryClient).trackEvent(
          eq("approved-premises-email-address-updated"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
            assertThat(it["emailAddressId"]).isEqualTo(emailAddressId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Create approved premises")
  @Nested
  inner class CreateApprovedPremises {
    val createApprovedPremisesRequest = CreateApprovedPremisesDto(
      approvedPremisesId = "NEWAPR",
      approvedPremisesName = "New Approved Premises",
      description = "The New Approved Premises",
      contact = "John Smith",
      active = true,
      accessibleAccess = AccessibleAccess.BY_ARRANGEMENT_ONLY,
      inactiveDate = null,
      cjitCode = "123456789",
      areaCode = "52",
      regionCode = "YOHUM",
      geographicalAreaCode = "WYORKS",
      localAuthorityCode = "00CG",
      payrollRegionCode = "NEY",
      addresses = listOf(
        UpdateAddressDto(
          addressLine1 = "Approved Premises House, 31 High Street",
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
      approvedPremisesRepository.findByIdOrNull(createApprovedPremisesRequest.approvedPremisesId)?.let { approvedPremisesRepository.delete(it) }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createApprovedPremisesRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createApprovedPremisesRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createApprovedPremisesRequest)
          .exchange()
          .expectStatus().isCreated
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `approved premises id already exists`() {
        dsl.approvedPremises(approvedPremisesId = createApprovedPremisesRequest.approvedPremisesId, name = "Existing Approved Premises") {}

        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createApprovedPremisesRequest)
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("Approved premises ${createApprovedPremisesRequest.approvedPremisesId} already exists")
      }

      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createApprovedPremisesRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for approved premises ${createApprovedPremisesRequest.approvedPremisesId}")
      }

      @Test
      fun `region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createApprovedPremisesRequest.copy(regionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for approved premises ${createApprovedPremisesRequest.approvedPremisesId}")
      }

      @Test
      fun `geographical area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createApprovedPremisesRequest.copy(geographicalAreaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for approved premises ${createApprovedPremisesRequest.approvedPremisesId}")
      }

      @Test
      fun `local authority code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createApprovedPremisesRequest.copy(localAuthorityCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for approved premises ${createApprovedPremisesRequest.approvedPremisesId}")
      }

      @Test
      fun `payroll region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createApprovedPremisesRequest.copy(payrollRegionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for approved premises ${createApprovedPremisesRequest.approvedPremisesId}")
      }

      @Test
      fun `approved premises name is blank`() {
        webTestClient.post()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createApprovedPremisesRequest.copy(approvedPremisesName = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will persist the approved premises, address, email address and phone number`() {
        webTestClient.post()
          .uri("/approved-premises")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createApprovedPremisesRequest)
          .exchange()
          .expectStatus().isCreated

        transactionHelper.runInTransaction {
          val persisted = approvedPremisesRepository.findByIdOrNull(createApprovedPremisesRequest.approvedPremisesId)

          assertThat(persisted).isNotNull
          assertThat(persisted!!.name).isEqualTo("New Approved Premises")
          assertThat(persisted.description).isEqualTo("The New Approved Premises")
          assertThat(persisted.contact).isEqualTo("John Smith")
          assertThat(persisted.active).isTrue
          assertThat(persisted.accessibleAccess).isEqualTo(AccessibleAccess.BY_ARRANGEMENT_ONLY)
          assertThat(persisted.cjitCode).isEqualTo("123456789")
          assertThat(persisted.area?.code).isEqualTo("52")
          assertThat(persisted.region?.code).isEqualTo("YOHUM")
          assertThat(persisted.geographicalArea?.code).isEqualTo("WYORKS")
          assertThat(persisted.localAuthority?.code).isEqualTo("00CG")
          assertThat(persisted.payrollRegion?.code).isEqualTo("NEY")

          assertThat(persisted.addresses).hasSize(1)
          assertThat(persisted.addresses[0].addressLine1).isEqualTo("Approved Premises House, 31 High Street")
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

        verify(telemetryClient).trackEvent(
          eq("approved-premises-created"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo(createApprovedPremisesRequest.approvedPremisesId)
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Create approved premises address")
  @Nested
  inner class CreateApprovedPremisesAddress {
    lateinit var approvedPremises: ApprovedPremises

    val createAddressRequest = UpdateAddressDto(
      addressLine1 = "Approved Premises House, 31 High Street",
      addressLine2 = "City Centre",
      town = "Sheffield",
      county = "South Yorkshire",
      postcode = "S1 3GG",
      country = "England",
    )

    @BeforeEach
    fun setUp() {
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
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
          addressLine1 = "Existing Approved Premises House",
          town = "Leeds",
          postcode = "LS1 1AA",
          country = "England",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      if (::approvedPremises.isInitialized) {
        approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/address")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/address")
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
      fun `404 if approved premises not found`() {
        webTestClient.post()
          .uri("/approved-premises/id/ZZZZ/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if town is missing`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(mapOf("postcode" to "S1 3GG", "country" to "England"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if postcode is too long`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/address")
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
      fun `will persist the new address against the approved premises`() {
        val addressDto: AgencyAddressDto = webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(addressDto.addressLine1).isEqualTo("Approved Premises House, 31 High Street")
        assertThat(addressDto.addressLine2).isEqualTo("City Centre")
        assertThat(addressDto.town).isEqualTo("Sheffield")
        assertThat(addressDto.county).isEqualTo("South Yorkshire")
        assertThat(addressDto.postcode).isEqualTo("S1 3GG")
        assertThat(addressDto.country).isEqualTo("England")
        assertThat(addressDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persisted = approvedPremisesRepository.findByIdOrNull("SHEFAP")!!
          assertThat(persisted.addresses).hasSize(2)
          val persistedAddress = persisted.addresses.find { it.id == addressDto.id }
          assertThat(persistedAddress).isNotNull
          assertThat(persistedAddress!!.addressLine1).isEqualTo("Approved Premises House, 31 High Street")
        }

        verify(telemetryClient).trackEvent(
          eq("approved-premises-address-created"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
            assertThat(it["addressId"]).isEqualTo(addressDto.id.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Create approved premises phone number")
  @Nested
  inner class CreateApprovedPremisesPhoneNumber {
    lateinit var approvedPremises: ApprovedPremises

    val createPhoneNumberRequest = UpdatePhoneNumberDto(number = "0114 555 8989")

    @BeforeEach
    fun setUp() {
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
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
      if (::approvedPremises.isInitialized) {
        approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
      }
      approvedPremisesRepository.findByIdOrNull("OTHAP")?.let { approvedPremisesRepository.deleteById("OTHAP") }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/phone-number")
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
      fun `404 if approved premises not found`() {
        webTestClient.post()
          .uri("/approved-premises/id/ZZZZ/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if phone number is in an incorrect format`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest.copy(number = "not-a-number"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if phone number is blank`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest.copy(number = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will allow the same phone number to be used by a different approved premises`() {
        dsl.approvedPremises(approvedPremisesId = "OTHAP", name = "Other Approved Premises") {}

        val phoneDto: AgencyPhoneDto = webTestClient.post()
          .uri("/approved-premises/id/OTHAP/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        verify(telemetryClient).trackEvent(
          eq("approved-premises-phone-number-created"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("OTHAP")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneDto.id.toString())
          },
          isNull(),
        )
      }

      @Test
      fun `will persist the new phone number against the approved premises`() {
        val phoneDto: AgencyPhoneDto = webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(phoneDto.number).isEqualTo("0114 555 8989")
        assertThat(phoneDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persisted = approvedPremisesRepository.findByIdOrNull("SHEFAP")!!
          assertThat(persisted.phoneNumbers).hasSize(2)
          val persistedPhoneNumber = persisted.phoneNumbers.find { it.id == phoneDto.id }
          assertThat(persistedPhoneNumber).isNotNull
          assertThat(persistedPhoneNumber!!.value).isEqualTo("0114 555 8989")
        }

        verify(telemetryClient).trackEvent(
          eq("approved-premises-phone-number-created"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneDto.id.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Create approved premises email address")
  @Nested
  inner class CreateApprovedPremisesEmailAddress {
    lateinit var approvedPremises: ApprovedPremises

    val createEmailAddressRequest = UpdateEmailAddressDto(address = "new@justice.gov.uk")

    @BeforeEach
    fun setUp() {
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
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
      if (::approvedPremises.isInitialized) {
        approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/email-address")
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
      fun `404 if approved premises not found`() {
        webTestClient.post()
          .uri("/approved-premises/id/ZZZZ/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if email address is in an incorrect format`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest.copy(address = "not-an-email"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if email address is blank`() {
        webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest.copy(address = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will persist the new email address against the approved premises`() {
        val emailDto: AgencyEmailDto = webTestClient.post()
          .uri("/approved-premises/id/SHEFAP/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(emailDto.address).isEqualTo("new@justice.gov.uk")
        assertThat(emailDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persisted = approvedPremisesRepository.findByIdOrNull("SHEFAP")!!
          assertThat(persisted.emailAddresses).hasSize(2)
          val persistedEmailAddress = persisted.emailAddresses.find { it.id == emailDto.id }
          assertThat(persistedEmailAddress).isNotNull
          assertThat(persistedEmailAddress!!.value).isEqualTo("new@justice.gov.uk")
        }

        verify(telemetryClient).trackEvent(
          eq("approved-premises-email-address-created"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
            assertThat(it["emailAddressId"]).isEqualTo(emailDto.id.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Delete approved premises")
  @Nested
  inner class DeleteApprovedPremises {
    lateinit var approvedPremises: ApprovedPremises

    @BeforeEach
    fun setUp() {
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
        active = true,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Approved Premises House, 31 High Street",
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
      if (::approvedPremises.isInitialized) {
        approvedPremisesRepository.findByIdOrNull(approvedPremises.approvedPremisesId)?.let { approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId) }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/approved-premises/id/SHEFAP")
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
      fun `will delete the approved premises, along with its addresses, emails and phone numbers`() {
        val addressId = approvedPremises.addresses[0].id
        val emailAddressId = approvedPremises.emailAddresses[0].id
        val phoneNumberId = approvedPremises.phoneNumbers[0].id

        webTestClient.delete()
          .uri("/approved-premises/id/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(approvedPremisesRepository.findByIdOrNull("SHEFAP")).isNull()
          assertThat(agencyAddressRepository.findByIdOrNull(addressId)).isNull()
          assertThat(emailAddressRepository.findByIdOrNull(emailAddressId)).isNull()
          assertThat(phoneNumberRepository.findByIdOrNull(phoneNumberId)).isNull()
        }

        verify(telemetryClient).trackEvent(
          eq("approved-premises-deleted"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Delete approved premises address")
  @Nested
  inner class DeleteApprovedPremisesAddress {
    lateinit var approvedPremises: ApprovedPremises
    var addressId: Long = -1

    @BeforeEach
    fun setUp() {
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
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
          addressLine1 = "Approved Premises House, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
      }
      addressId = approvedPremises.addresses[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::approvedPremises.isInitialized) {
        approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/approved-premises/id/SHEFAP/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/approved-premises/id/SHEFAP/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if approved premises not found`() {
        webTestClient.delete()
          .uri("/approved-premises/id/ZZZZ/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if address not found`() {
        webTestClient.delete()
          .uri("/approved-premises/id/SHEFAP/address/{addressId}", 999999)
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
          .uri("/approved-premises/id/SHEFAP/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(agencyAddressRepository.findByIdOrNull(addressId)).isNull()
          assertThat(approvedPremisesRepository.findByIdOrNull("SHEFAP")?.addresses).isEmpty()
        }

        verify(telemetryClient).trackEvent(
          eq("approved-premises-address-deleted"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
            assertThat(it["addressId"]).isEqualTo(addressId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Delete approved premises phone number")
  @Nested
  inner class DeleteApprovedPremisesPhoneNumber {
    lateinit var approvedPremises: ApprovedPremises
    var phoneNumberId: Long = -1

    @BeforeEach
    fun setUp() {
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
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
      phoneNumberId = approvedPremises.phoneNumbers[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::approvedPremises.isInitialized) {
        approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/approved-premises/id/SHEFAP/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/approved-premises/id/SHEFAP/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if approved premises not found`() {
        webTestClient.delete()
          .uri("/approved-premises/id/ZZZZ/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if phone number not found`() {
        webTestClient.delete()
          .uri("/approved-premises/id/SHEFAP/phone-number/{phoneNumberId}", 999999)
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
          .uri("/approved-premises/id/SHEFAP/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(phoneNumberRepository.findByIdOrNull(phoneNumberId)).isNull()
          assertThat(approvedPremisesRepository.findByIdOrNull("SHEFAP")?.phoneNumbers).isEmpty()
        }

        verify(telemetryClient).trackEvent(
          eq("approved-premises-phone-number-deleted"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneNumberId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Delete approved premises email address")
  @Nested
  inner class DeleteApprovedPremisesEmailAddress {
    lateinit var approvedPremises: ApprovedPremises
    var emailAddressId: Long = -1

    @BeforeEach
    fun setUp() {
      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "SHEFAP",
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
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
      emailAddressId = approvedPremises.emailAddresses[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::approvedPremises.isInitialized) {
        approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/approved-premises/id/SHEFAP/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/approved-premises/id/SHEFAP/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if approved premises not found`() {
        webTestClient.delete()
          .uri("/approved-premises/id/ZZZZ/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if email address not found`() {
        webTestClient.delete()
          .uri("/approved-premises/id/SHEFAP/email-address/{emailAddressId}", 999999)
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
          .uri("/approved-premises/id/SHEFAP/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(emailAddressRepository.findByIdOrNull(emailAddressId)).isNull()
          assertThat(approvedPremisesRepository.findByIdOrNull("SHEFAP")?.emailAddresses).isEmpty()
        }

        verify(telemetryClient).trackEvent(
          eq("approved-premises-email-address-deleted"),
          check {
            assertThat(it["approvedPremisesId"]).isEqualTo("SHEFAP")
            assertThat(it["emailAddressId"]).isEqualTo(emailAddressId.toString())
          },
          isNull(),
        )
      }
    }
  }
}
