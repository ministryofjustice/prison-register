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
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumberRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOffice
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class ProbationOfficeResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var probationOfficeRepository: ProbationOfficeRepository

  @Autowired
  lateinit var agencyAddressRepository: AgencyAddressRepository

  @Autowired
  lateinit var emailAddressRepository: EmailAddressRepository

  @Autowired
  lateinit var phoneNumberRepository: PhoneNumberRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

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
        contact = "Jane Smith",
        active = false,
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        subareaCode = "SHEFF",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
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
        assertThat(probationOfficeDto.contact).isEqualTo("Jane Smith")
        assertThat(probationOfficeDto.active).isFalse
        assertThat(probationOfficeDto.accessibleAccess).isEqualTo("ACCESSIBLE")
        assertThat(probationOfficeDto.inactiveDate).isEqualTo("2020-01-02")
        assertThat(probationOfficeDto.area?.description).isEqualTo("South Yorkshire")
        assertThat(probationOfficeDto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(probationOfficeDto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(probationOfficeDto.localAuthority?.description).isEqualTo("Sheffield City Council")
        assertThat(probationOfficeDto.payrollRegion?.code).isEqualTo("NEY")
        assertThat(probationOfficeDto.subarea?.description).isEqualTo("Sheffield")
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

  @DisplayName("Get all probation offices")
  @Nested
  inner class GetAll {
    lateinit var probationOffice: ProbationOffice
    lateinit var probationOffice2: ProbationOffice
    lateinit var probationOffice3: ProbationOffice

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
        active = false,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {}

      probationOffice2 = dsl.probationOffice(
        probationOfficeId = "LEEDPB",
        name = "Leeds Probation Office",
      ) {}

      probationOffice3 = dsl.probationOffice(
        probationOfficeId = "BIRMPB",
        name = "Birmingham Probation Office",
      ) {}
    }

    @AfterEach
    fun tearDown() {
      probationOfficeRepository.deleteById(probationOffice.probationOfficeId)
      probationOfficeRepository.deleteById(probationOffice2.probationOfficeId)
      probationOfficeRepository.deleteById(probationOffice3.probationOfficeId)
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return all probation offices`() {
        val probationOffices = webTestClient.get()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyList(ProbationOfficeDto::class.java)
          .returnResult()
          .responseBody!!

        assertThat(probationOffices).extracting("probationOfficeId").contains("SHEFPB", "LEEDPB", "BIRMPB")

        val probationOfficeDto = probationOffices.first { it.probationOfficeId == "SHEFPB" }
        assertThat(probationOfficeDto.probationOfficeName).isEqualTo("Sheffield Probation Office")
        assertThat(probationOfficeDto.description).isEqualTo("Sheffield City Centre Probation Office")
        assertThat(probationOfficeDto.active).isFalse

        val probationOffice2Dto = probationOffices.first { it.probationOfficeId == "LEEDPB" }
        assertThat(probationOffice2Dto.probationOfficeName).isEqualTo("Leeds Probation Office")

        val probationOffice3Dto = probationOffices.first { it.probationOfficeId == "BIRMPB" }
        assertThat(probationOffice3Dto.probationOfficeName).isEqualTo("Birmingham Probation Office")
      }
    }
  }

  @DisplayName("Update probation office")
  @Nested
  inner class UpdateProbationOffice {
    lateinit var probationOffice: ProbationOffice

    val updateProbationOfficeRequest = UpdateProbationOfficeDto(
      probationOfficeName = "Sheffield Central Probation Office",
      description = "Sheffield City Probation Office",
      contact = "Alex Taylor",
      active = true,
      accessibleAccess = AccessibleAccess.WHEELCHAIR_ACCESS,
      inactiveDate = null,
      cjitCode = "123456789",
      areaCode = "52",
      subareaCode = "SHEFF",
      regionCode = "YOHUM",
      geographicalAreaCode = "WYORKS",
      localAuthorityCode = "00CG",
      payrollRegionCode = "NEY",
    )

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
        contact = "Jane Smith",
        active = false,
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        subareaCode = "SHEFF",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Probation House, 31 High Street",
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
      if (::probationOffice.isInitialized) {
        probationOfficeRepository.deleteById(probationOffice.probationOfficeId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateProbationOfficeRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateProbationOfficeRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateProbationOfficeRequest)
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if not found`() {
        webTestClient.put()
          .uri("/probation-offices/id/ZZZZ")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateProbationOfficeRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateProbationOfficeRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for probation office SHEFPB")
      }

      @Test
      fun `subarea code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateProbationOfficeRequest.copy(subareaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ subarea code not found for probation office SHEFPB")
      }

      @Test
      fun `region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateProbationOfficeRequest.copy(regionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for probation office SHEFPB")
      }

      @Test
      fun `geographical area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateProbationOfficeRequest.copy(geographicalAreaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for probation office SHEFPB")
      }

      @Test
      fun `local authority code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateProbationOfficeRequest.copy(localAuthorityCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for probation office SHEFPB")
      }

      @Test
      fun `payroll region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateProbationOfficeRequest.copy(payrollRegionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for probation office SHEFPB")
      }

      @Test
      fun `probation office name is blank`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateProbationOfficeRequest.copy(probationOfficeName = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the core probation office data`() {
        val dto: ProbationOfficeDto = webTestClient.put()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateProbationOfficeRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(dto.probationOfficeId).isEqualTo("SHEFPB")
        assertThat(dto.probationOfficeName).isEqualTo("Sheffield Central Probation Office")
        assertThat(dto.description).isEqualTo("Sheffield City Probation Office")
        assertThat(dto.contact).isEqualTo("Alex Taylor")
        assertThat(dto.active).isFalse
        assertThat(dto.accessibleAccess).isEqualTo("WHEELCHAIR_ACCESS")
        assertThat(dto.inactiveDate).isEqualTo("2026-01-01")
        assertThat(dto.cjitCode).isEqualTo("123456789")
        assertThat(dto.area?.description).isEqualTo("South Yorkshire")
        assertThat(dto.subarea?.description).isEqualTo("Sheffield")
        assertThat(dto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(dto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(dto.localAuthority?.description).isEqualTo("Sheffield City Council")
        assertThat(dto.payrollRegion?.code).isEqualTo("NEY")
      }

      @Test
      fun `will not affect addresses, emails or phone numbers`() {
        val dto: ProbationOfficeDto = webTestClient.put()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateProbationOfficeRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(dto.addresses).hasSize(1)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("Probation House, 31 High Street")
        assertThat(dto.emailAddresses).hasSize(1)
        assertThat(dto.emailAddresses[0].address).isEqualTo("test@justice.gov.uk")
        assertThat(dto.phoneNumbers).hasSize(1)
        assertThat(dto.phoneNumbers[0].number).isEqualTo("0114 555 8989")
      }
    }
  }

  @DisplayName("Update probation office address")
  @Nested
  inner class UpdateProbationOfficeAddress {
    lateinit var probationOffice: ProbationOffice
    var addressId: Long = -1

    val updateAddressRequest = UpdateAddressDto(
      addressLine1 = "Updated Probation House, 31 High Street",
      addressLine2 = "Updated City Centre",
      town = "Updated Sheffield",
      county = "Updated South Yorkshire",
      postcode = "S1 4HH",
      country = "Wales",
    )

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
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
          addressLine1 = "Probation House, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
      }
      addressId = probationOffice.addresses[0].id
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
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/address/{addressId}", addressId)
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
      fun `404 if probation office not found`() {
        webTestClient.put()
          .uri("/probation-offices/id/ZZZZ/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if address not found`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/address/{addressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if town is missing`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(mapOf("postcode" to "S1 3GG", "country" to "England"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if postcode is too long`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/address/{addressId}", addressId)
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
          .uri("/probation-offices/id/SHEFPB/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(addressDto.id).isEqualTo(addressId)
        assertThat(addressDto.addressLine1).isEqualTo("Updated Probation House, 31 High Street")
        assertThat(addressDto.addressLine2).isEqualTo("Updated City Centre")
        assertThat(addressDto.town).isEqualTo("Updated Sheffield")
        assertThat(addressDto.county).isEqualTo("Updated South Yorkshire")
        assertThat(addressDto.postcode).isEqualTo("S1 4HH")
        assertThat(addressDto.country).isEqualTo("Wales")
      }
    }
  }

  @DisplayName("Update probation office phone number")
  @Nested
  inner class UpdateProbationOfficePhoneNumber {
    lateinit var probationOffice: ProbationOffice
    var phoneNumberId: Long = -1

    val updatePhoneNumberRequest = UpdatePhoneNumberDto(number = "0114 555 1234")

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
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
      phoneNumberId = probationOffice.phoneNumbers.first { it.value == "0114 555 8989" }.id
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
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", phoneNumberId)
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
      fun `404 if probation office not found`() {
        webTestClient.put()
          .uri("/probation-offices/id/ZZZZ/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if phone number not found`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if phone number is in an incorrect format`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = "not-a-number"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if phone number is blank`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = ""))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `409 if phone number already exists on this probation office`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = "0114 555 4321"))
          .exchange()
          .expectStatus().isEqualTo(409).expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("Phone number 0114 555 4321 already exists")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the phone number and preserve its id`() {
        val phoneDto: AgencyPhoneDto = webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(phoneDto.id).isEqualTo(phoneNumberId)
        assertThat(phoneDto.number).isEqualTo("0114 555 1234")
      }

      @Test
      fun `will allow updating a phone number to its own current value`() {
        val phoneDto: AgencyPhoneDto = webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = "0114 555 8989"))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(phoneDto.number).isEqualTo("0114 555 8989")
      }
    }
  }

  @DisplayName("Update probation office email address")
  @Nested
  inner class UpdateProbationOfficeEmailAddress {
    lateinit var probationOffice: ProbationOffice
    var emailAddressId: Long = -1

    val updateEmailAddressRequest = UpdateEmailAddressDto(address = "updated@justice.gov.uk")

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
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
      emailAddressId = probationOffice.emailAddresses[0].id
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
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/email-address/{emailAddressId}", emailAddressId)
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
      fun `404 if probation office not found`() {
        webTestClient.put()
          .uri("/probation-offices/id/ZZZZ/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if email address not found`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/email-address/{emailAddressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if email address is in an incorrect format`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest.copy(address = "not-an-email"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if email address is blank`() {
        webTestClient.put()
          .uri("/probation-offices/id/SHEFPB/email-address/{emailAddressId}", emailAddressId)
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
          .uri("/probation-offices/id/SHEFPB/email-address/{emailAddressId}", emailAddressId)
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

  @DisplayName("Create probation office")
  @Nested
  inner class CreateProbationOffice {
    val createProbationOfficeRequest = CreateProbationOfficeDto(
      probationOfficeId = "NEWPBO",
      probationOfficeName = "New Probation Office",
      description = "The New Probation Office",
      contact = "John Smith",
      active = true,
      accessibleAccess = AccessibleAccess.BY_ARRANGEMENT_ONLY,
      inactiveDate = null,
      cjitCode = "123456789",
      areaCode = "52",
      subareaCode = "SHEFF",
      regionCode = "YOHUM",
      geographicalAreaCode = "WYORKS",
      localAuthorityCode = "00CG",
      payrollRegionCode = "NEY",
      addresses = listOf(
        UpdateAddressDto(
          addressLine1 = "Probation House, 31 High Street",
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
      probationOfficeRepository.findByIdOrNull(createProbationOfficeRequest.probationOfficeId)?.let { probationOfficeRepository.delete(it) }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createProbationOfficeRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createProbationOfficeRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createProbationOfficeRequest)
          .exchange()
          .expectStatus().isCreated
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `probation office id already exists`() {
        dsl.probationOffice(probationOfficeId = createProbationOfficeRequest.probationOfficeId, name = "Existing Probation Office") {}

        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createProbationOfficeRequest)
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("Probation office ${createProbationOfficeRequest.probationOfficeId} already exists")
      }

      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createProbationOfficeRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for probation office ${createProbationOfficeRequest.probationOfficeId}")
      }

      @Test
      fun `subarea code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createProbationOfficeRequest.copy(subareaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ subarea code not found for probation office ${createProbationOfficeRequest.probationOfficeId}")
      }

      @Test
      fun `region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createProbationOfficeRequest.copy(regionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for probation office ${createProbationOfficeRequest.probationOfficeId}")
      }

      @Test
      fun `geographical area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createProbationOfficeRequest.copy(geographicalAreaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for probation office ${createProbationOfficeRequest.probationOfficeId}")
      }

      @Test
      fun `local authority code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createProbationOfficeRequest.copy(localAuthorityCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for probation office ${createProbationOfficeRequest.probationOfficeId}")
      }

      @Test
      fun `payroll region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createProbationOfficeRequest.copy(payrollRegionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for probation office ${createProbationOfficeRequest.probationOfficeId}")
      }

      @Test
      fun `probation office name is blank`() {
        webTestClient.post()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createProbationOfficeRequest.copy(probationOfficeName = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will persist the probation office, address, email address and phone number`() {
        webTestClient.post()
          .uri("/probation-offices")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createProbationOfficeRequest)
          .exchange()
          .expectStatus().isCreated

        transactionHelper.runInTransaction {
          val persisted = probationOfficeRepository.findByIdOrNull(createProbationOfficeRequest.probationOfficeId)

          assertThat(persisted).isNotNull
          assertThat(persisted!!.name).isEqualTo("New Probation Office")
          assertThat(persisted.description).isEqualTo("The New Probation Office")
          assertThat(persisted.contact).isEqualTo("John Smith")
          assertThat(persisted.active).isTrue
          assertThat(persisted.accessibleAccess).isEqualTo(AccessibleAccess.BY_ARRANGEMENT_ONLY)
          assertThat(persisted.cjitCode).isEqualTo("123456789")
          assertThat(persisted.area?.code).isEqualTo("52")
          assertThat(persisted.subarea?.code).isEqualTo("SHEFF")
          assertThat(persisted.region?.code).isEqualTo("YOHUM")
          assertThat(persisted.geographicalArea?.code).isEqualTo("WYORKS")
          assertThat(persisted.localAuthority?.code).isEqualTo("00CG")
          assertThat(persisted.payrollRegion?.code).isEqualTo("NEY")

          assertThat(persisted.addresses).hasSize(1)
          assertThat(persisted.addresses[0].addressLine1).isEqualTo("Probation House, 31 High Street")
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

  @DisplayName("Create probation office address")
  @Nested
  inner class CreateProbationOfficeAddress {
    lateinit var probationOffice: ProbationOffice

    val createAddressRequest = UpdateAddressDto(
      addressLine1 = "Probation House, 31 High Street",
      addressLine2 = "City Centre",
      town = "Sheffield",
      county = "South Yorkshire",
      postcode = "S1 3GG",
      country = "England",
    )

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
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
          addressLine1 = "Existing Probation House",
          town = "Leeds",
          postcode = "LS1 1AA",
          country = "England",
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
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/address")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/address")
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
      fun `404 if probation office not found`() {
        webTestClient.post()
          .uri("/probation-offices/id/ZZZZ/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if town is missing`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(mapOf("postcode" to "S1 3GG", "country" to "England"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if postcode is too long`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/address")
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
      fun `will persist the new address against the probation office`() {
        val addressDto: AgencyAddressDto = webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(addressDto.addressLine1).isEqualTo("Probation House, 31 High Street")
        assertThat(addressDto.addressLine2).isEqualTo("City Centre")
        assertThat(addressDto.town).isEqualTo("Sheffield")
        assertThat(addressDto.county).isEqualTo("South Yorkshire")
        assertThat(addressDto.postcode).isEqualTo("S1 3GG")
        assertThat(addressDto.country).isEqualTo("England")
        assertThat(addressDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persisted = probationOfficeRepository.findByIdOrNull("SHEFPB")!!
          assertThat(persisted.addresses).hasSize(2)
          val persistedAddress = persisted.addresses.find { it.id == addressDto.id }
          assertThat(persistedAddress).isNotNull
          assertThat(persistedAddress!!.addressLine1).isEqualTo("Probation House, 31 High Street")
        }
      }
    }
  }

  @DisplayName("Create probation office phone number")
  @Nested
  inner class CreateProbationOfficePhoneNumber {
    lateinit var probationOffice: ProbationOffice

    val createPhoneNumberRequest = UpdatePhoneNumberDto(number = "0114 555 8989")

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
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
      if (::probationOffice.isInitialized) {
        probationOfficeRepository.deleteById(probationOffice.probationOfficeId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/phone-number")
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
      fun `404 if probation office not found`() {
        webTestClient.post()
          .uri("/probation-offices/id/ZZZZ/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if phone number is in an incorrect format`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest.copy(number = "not-a-number"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if phone number is blank`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest.copy(number = ""))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `409 if phone number already exists`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated

        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/phone-number")
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
      fun `will allow the same phone number to be used by a different probation office`() {
        dsl.probationOffice(probationOfficeId = "OTHPBO", name = "Other Probation Office") {}

        webTestClient.post()
          .uri("/probation-offices/id/OTHPBO/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated

        probationOfficeRepository.deleteById("OTHPBO")
      }

      @Test
      fun `will persist the new phone number against the probation office`() {
        val phoneDto: AgencyPhoneDto = webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(phoneDto.number).isEqualTo("0114 555 8989")
        assertThat(phoneDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persisted = probationOfficeRepository.findByIdOrNull("SHEFPB")!!
          assertThat(persisted.phoneNumbers).hasSize(2)
          val persistedPhoneNumber = persisted.phoneNumbers.find { it.id == phoneDto.id }
          assertThat(persistedPhoneNumber).isNotNull
          assertThat(persistedPhoneNumber!!.value).isEqualTo("0114 555 8989")
        }
      }
    }
  }

  @DisplayName("Create probation office email address")
  @Nested
  inner class CreateProbationOfficeEmailAddress {
    lateinit var probationOffice: ProbationOffice

    val createEmailAddressRequest = UpdateEmailAddressDto(address = "new@justice.gov.uk")

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
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
      if (::probationOffice.isInitialized) {
        probationOfficeRepository.deleteById(probationOffice.probationOfficeId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/email-address")
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
      fun `404 if probation office not found`() {
        webTestClient.post()
          .uri("/probation-offices/id/ZZZZ/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if email address is in an incorrect format`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest.copy(address = "not-an-email"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if email address is blank`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest.copy(address = ""))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `409 if email address already exists`() {
        webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isCreated

        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/email-address")
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
      fun `will persist the new email address against the probation office`() {
        val emailDto: AgencyEmailDto = webTestClient.post()
          .uri("/probation-offices/id/SHEFPB/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(emailDto.address).isEqualTo("new@justice.gov.uk")
        assertThat(emailDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persisted = probationOfficeRepository.findByIdOrNull("SHEFPB")!!
          assertThat(persisted.emailAddresses).hasSize(2)
          val persistedEmailAddress = persisted.emailAddresses.find { it.id == emailDto.id }
          assertThat(persistedEmailAddress).isNotNull
          assertThat(persistedEmailAddress!!.value).isEqualTo("new@justice.gov.uk")
        }
      }
    }
  }

  @DisplayName("Delete probation office")
  @Nested
  inner class DeleteProbationOffice {
    lateinit var probationOffice: ProbationOffice

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
        active = true,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Probation House, 31 High Street",
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
      if (::probationOffice.isInitialized) {
        probationOfficeRepository.findByIdOrNull(probationOffice.probationOfficeId)?.let { probationOfficeRepository.deleteById(probationOffice.probationOfficeId) }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/probation-offices/id/SHEFPB")
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
      fun `will delete the probation office, along with its addresses, emails and phone numbers`() {
        val addressId = probationOffice.addresses[0].id
        val emailAddressId = probationOffice.emailAddresses[0].id
        val phoneNumberId = probationOffice.phoneNumbers[0].id

        webTestClient.delete()
          .uri("/probation-offices/id/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")).isNull()
          assertThat(agencyAddressRepository.findByIdOrNull(addressId)).isNull()
          assertThat(emailAddressRepository.findByIdOrNull(emailAddressId)).isNull()
          assertThat(phoneNumberRepository.findByIdOrNull(phoneNumberId)).isNull()
        }
      }
    }
  }

  @DisplayName("Delete probation office address")
  @Nested
  inner class DeleteProbationOfficeAddress {
    lateinit var probationOffice: ProbationOffice
    var addressId: Long = -1

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
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
          addressLine1 = "Probation House, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
      }
      addressId = probationOffice.addresses[0].id
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
        webTestClient.delete()
          .uri("/probation-offices/id/SHEFPB/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/probation-offices/id/SHEFPB/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if probation office not found`() {
        webTestClient.delete()
          .uri("/probation-offices/id/ZZZZ/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if address not found`() {
        webTestClient.delete()
          .uri("/probation-offices/id/SHEFPB/address/{addressId}", 999999)
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
          .uri("/probation-offices/id/SHEFPB/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(agencyAddressRepository.findByIdOrNull(addressId)).isNull()
          assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")?.addresses).isEmpty()
        }
      }
    }
  }

  @DisplayName("Delete probation office phone number")
  @Nested
  inner class DeleteProbationOfficePhoneNumber {
    lateinit var probationOffice: ProbationOffice
    var phoneNumberId: Long = -1

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
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
      phoneNumberId = probationOffice.phoneNumbers[0].id
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
        webTestClient.delete()
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if probation office not found`() {
        webTestClient.delete()
          .uri("/probation-offices/id/ZZZZ/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if phone number not found`() {
        webTestClient.delete()
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", 999999)
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
          .uri("/probation-offices/id/SHEFPB/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(phoneNumberRepository.findByIdOrNull(phoneNumberId)).isNull()
          assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")?.phoneNumbers).isEmpty()
        }
      }
    }
  }

  @DisplayName("Delete probation office email address")
  @Nested
  inner class DeleteProbationOfficeEmailAddress {
    lateinit var probationOffice: ProbationOffice
    var emailAddressId: Long = -1

    @BeforeEach
    fun setUp() {
      probationOffice = dsl.probationOffice(
        probationOfficeId = "SHEFPB",
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
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
      emailAddressId = probationOffice.emailAddresses[0].id
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
        webTestClient.delete()
          .uri("/probation-offices/id/SHEFPB/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/probation-offices/id/SHEFPB/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if probation office not found`() {
        webTestClient.delete()
          .uri("/probation-offices/id/ZZZZ/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if email address not found`() {
        webTestClient.delete()
          .uri("/probation-offices/id/SHEFPB/email-address/{emailAddressId}", 999999)
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
          .uri("/probation-offices/id/SHEFPB/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(emailAddressRepository.findByIdOrNull(emailAddressId)).isNull()
          assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")?.emailAddresses).isEmpty()
        }
      }
    }
  }
}
