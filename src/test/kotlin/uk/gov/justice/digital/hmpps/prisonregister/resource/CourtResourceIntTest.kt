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
import uk.gov.justice.digital.hmpps.prisonregister.model.Court
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class CourtResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var courtRepository: CourtRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

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
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
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
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
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
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
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
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(courtDto.courtId).isEqualTo("SHEFCC")
        assertThat(courtDto.courtName).isEqualTo("Sheffield Central Ct")
        assertThat(courtDto.description).isEqualTo("Sheffield Central Court")
        assertThat(courtDto.active).isFalse
        assertThat(courtDto.inactiveDate).isEqualTo("2020-01-02")
        assertThat(courtDto.accessibleAccess).isEqualTo("ACCESSIBLE")
        assertThat(courtDto.courtType?.description).isEqualTo("Crown Court")
        assertThat(courtDto.area?.description).isEqualTo("South Yorkshire")
        assertThat(courtDto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(courtDto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(courtDto.localAuthority?.description).isEqualTo("Sheffield City Council")
        assertThat(courtDto.payrollRegion?.code).isEqualTo("NEY")
      }

      @Test
      fun `will return addresses `() {
        val courtDto: CourtDto = webTestClient.get()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
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
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
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
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(courtDto.phoneNumbers).hasSize(2)
        assertThat(courtDto.phoneNumbers[0].number).isEqualTo("0114 555 8989")
      }
    }
  }

  @DisplayName("Get all courts")
  @Nested
  inner class GetAll {
    lateinit var court: Court
    lateinit var court2: Court
    lateinit var court3: Court

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
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
      ) {}

      court2 = dsl.court(
        courtId = "LEEDCC",
        name = "Leeds Central Ct",
      ) {}

      court3 = dsl.court(
        courtId = "BIRMCC",
        name = "Birmingham Central Ct",
      ) {}
    }

    @AfterEach
    fun tearDown() {
      courtRepository.deleteById(court.courtId)
      courtRepository.deleteById(court2.courtId)
      courtRepository.deleteById(court3.courtId)
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/courts")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/courts")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/courts")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return all courts`() {
        val courts = webTestClient.get()
          .uri("/courts")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyList(CourtDto::class.java)
          .returnResult()
          .responseBody!!

        assertThat(courts).extracting("courtId").contains("SHEFCC", "LEEDCC", "BIRMCC")

        val courtDto = courts.first { it.courtId == "SHEFCC" }
        assertThat(courtDto.courtName).isEqualTo("Sheffield Central Ct")
        assertThat(courtDto.description).isEqualTo("Sheffield Central Court")
        assertThat(courtDto.active).isFalse

        val court2Dto = courts.first { it.courtId == "LEEDCC" }
        assertThat(court2Dto.courtName).isEqualTo("Leeds Central Ct")

        val court3Dto = courts.first { it.courtId == "BIRMCC" }
        assertThat(court3Dto.courtName).isEqualTo("Birmingham Central Ct")
      }
    }
  }

  @DisplayName("Update court")
  @Nested
  inner class UpdateCourt {
    lateinit var court: Court

    val updateCourtRequest = UpdateCourtDto(
      courtName = "Sheffield Magistrates Ct",
      description = "Sheffield Magistrates' Court",
      active = true,
      inactiveDate = null,
      cjitCode = "123456789",
      accessibleAccess = AccessibleAccess.WHEELCHAIR_ACCESS,
      areaCode = "52",
      regionCode = "YOHUM",
      geographicalAreaCode = "WYORKS",
      localAuthorityCode = "00CG",
      payrollRegionCode = "NEY",
      courtTypeCode = "MC",
    )

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
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
      ) {
        address(
          addressLine1 = "Court House, 31 High Street",
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
      if (::court.isInitialized) {
        courtRepository.deleteById(court.courtId)
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateCourtRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateCourtRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateCourtRequest)
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if not found`() {
        webTestClient.put()
          .uri("/courts/id/ZZZZ")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateCourtRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateCourtRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for court SHEFCC")
      }

      @Test
      fun `region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateCourtRequest.copy(regionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for court SHEFCC")
      }

      @Test
      fun `geographical area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateCourtRequest.copy(geographicalAreaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for court SHEFCC")
      }

      @Test
      fun `local authority code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateCourtRequest.copy(localAuthorityCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for court SHEFCC")
      }

      @Test
      fun `payroll region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateCourtRequest.copy(payrollRegionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for court SHEFCC")
      }

      @Test
      fun `court type code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateCourtRequest.copy(courtTypeCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ court type not found for court SHEFCC")
      }

      @Test
      fun `court name is blank`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateCourtRequest.copy(courtName = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the core court data`() {
        val courtDto: CourtDto = webTestClient.put()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateCourtRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(courtDto.courtId).isEqualTo("SHEFCC")
        assertThat(courtDto.courtName).isEqualTo("Sheffield Magistrates Ct")
        assertThat(courtDto.description).isEqualTo("Sheffield Magistrates' Court")
        assertThat(courtDto.active).isFalse
        assertThat(courtDto.inactiveDate).isEqualTo("2026-01-01")
        assertThat(courtDto.cjitCode).isEqualTo("123456789")
        assertThat(courtDto.accessibleAccess).isEqualTo("WHEELCHAIR_ACCESS")
        assertThat(courtDto.area?.description).isEqualTo("South Yorkshire")
        assertThat(courtDto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(courtDto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(courtDto.localAuthority?.description).isEqualTo("Sheffield City Council")
        assertThat(courtDto.payrollRegion?.code).isEqualTo("NEY")
        assertThat(courtDto.courtType?.description).isEqualTo("Magistrates Court")
      }

      @Test
      fun `will not affect addresses, emails or phone numbers`() {
        val courtDto: CourtDto = webTestClient.put()
          .uri("/courts/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateCourtRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(courtDto.addresses).hasSize(1)
        assertThat(courtDto.addresses[0].addressLine1).isEqualTo("Court House, 31 High Street")
        assertThat(courtDto.emailAddresses).hasSize(1)
        assertThat(courtDto.emailAddresses[0].address).isEqualTo("test@justice.gov.uk")
        assertThat(courtDto.phoneNumbers).hasSize(1)
        assertThat(courtDto.phoneNumbers[0].number).isEqualTo("0114 555 8989")
      }
    }
  }

  @DisplayName("Update court address")
  @Nested
  inner class UpdateCourtAddress {
    lateinit var court: Court
    var addressId: Long = -1

    val updateAddressRequest = UpdateAddressDto(
      addressLine1 = "Updated Court House, 31 High Street",
      addressLine2 = "Updated City Centre",
      town = "Updated Sheffield",
      county = "Updated South Yorkshire",
      postcode = "S1 4HH",
      country = "Wales",
    )

    @BeforeEach
    fun setUp() {
      court = dsl.court(
        courtId = "SHEFCC",
        name = "Sheffield Central Ct",
        description = "Sheffield Central Court",
        active = true,
        inactiveDate = null,
        courtTypeCode = "CC",
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
      ) {
        address(
          addressLine1 = "Court House, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
      }
      addressId = court.addresses[0].id
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
        webTestClient.put()
          .uri("/courts/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if court not found`() {
        webTestClient.put()
          .uri("/courts/id/ZZZZ/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if address not found`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/address/{addressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if town is missing`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(mapOf("postcode" to "S1 3GG", "country" to "England"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if postcode is too long`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
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
          .uri("/courts/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(addressDto.id).isEqualTo(addressId)
        assertThat(addressDto.addressLine1).isEqualTo("Updated Court House, 31 High Street")
        assertThat(addressDto.addressLine2).isEqualTo("Updated City Centre")
        assertThat(addressDto.town).isEqualTo("Updated Sheffield")
        assertThat(addressDto.county).isEqualTo("Updated South Yorkshire")
        assertThat(addressDto.postcode).isEqualTo("S1 4HH")
        assertThat(addressDto.country).isEqualTo("Wales")
      }
    }
  }

  @DisplayName("Update court phone number")
  @Nested
  inner class UpdateCourtPhoneNumber {
    lateinit var court: Court
    var phoneNumberId: Long = -1

    val updatePhoneNumberRequest = UpdatePhoneNumberDto(number = "0114 555 1234")

    @BeforeEach
    fun setUp() {
      court = dsl.court(
        courtId = "SHEFCC",
        name = "Sheffield Central Ct",
        description = "Sheffield Central Court",
        active = true,
        inactiveDate = null,
        courtTypeCode = "CC",
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
      ) {
        phoneNumber(
          phoneNumber = "0114 555 8989",
        )
      }
      phoneNumberId = court.phoneNumbers[0].id
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
        webTestClient.put()
          .uri("/courts/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if court not found`() {
        webTestClient.put()
          .uri("/courts/id/ZZZZ/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if phone number not found`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/phone-number/{phoneNumberId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if phone number is in an incorrect format`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = "not-a-number"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if phone number is blank`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
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
          .uri("/courts/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(phoneDto.id).isEqualTo(phoneNumberId)
        assertThat(phoneDto.number).isEqualTo("0114 555 1234")
      }
    }
  }

  @DisplayName("Update court email address")
  @Nested
  inner class UpdateCourtEmailAddress {
    lateinit var court: Court
    var emailAddressId: Long = -1

    val updateEmailAddressRequest = UpdateEmailAddressDto(address = "updated@justice.gov.uk")

    @BeforeEach
    fun setUp() {
      court = dsl.court(
        courtId = "SHEFCC",
        name = "Sheffield Central Ct",
        description = "Sheffield Central Court",
        active = true,
        inactiveDate = null,
        courtTypeCode = "CC",
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
      ) {
        email(
          emailAddress = "test@justice.gov.uk",
        )
      }
      emailAddressId = court.emailAddresses[0].id
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
        webTestClient.put()
          .uri("/courts/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if court not found`() {
        webTestClient.put()
          .uri("/courts/id/ZZZZ/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if email address not found`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/email-address/{emailAddressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if email address is in an incorrect format`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateEmailAddressRequest.copy(address = "not-an-email"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if email address is blank`() {
        webTestClient.put()
          .uri("/courts/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
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
          .uri("/courts/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(emailDto.id).isEqualTo(emailAddressId)
        assertThat(emailDto.address).isEqualTo("updated@justice.gov.uk")
      }
    }
  }

  @DisplayName("Create court")
  @Nested
  inner class CreateCourt {
    val createCourtRequest = CreateCourtDto(
      courtId = "NEWCRT",
      courtName = "New Court",
      description = "The New Court",
      active = true,
      inactiveDate = null,
      cjitCode = "123456789",
      accessibleAccess = AccessibleAccess.ACCESSIBLE,
      areaCode = "52",
      regionCode = "YOHUM",
      geographicalAreaCode = "WYORKS",
      localAuthorityCode = "00CG",
      payrollRegionCode = "NEY",
      courtTypeCode = "CC",
      addresses = listOf(
        UpdateAddressDto(
          addressLine1 = "Court House, 31 High Street",
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
      courtRepository.findByIdOrNull(createCourtRequest.courtId)?.let { courtRepository.delete(it) }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/courts")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createCourtRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/courts")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createCourtRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/courts")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(createCourtRequest)
          .exchange()
          .expectStatus().isCreated
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `court id already exists`() {
        dsl.court(courtId = createCourtRequest.courtId, name = "Existing Court") {}

        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/courts")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(createCourtRequest)
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("Court ${createCourtRequest.courtId} already exists")
      }

      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/courts")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(createCourtRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for court ${createCourtRequest.courtId}")
      }

      @Test
      fun `court type code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/courts")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(createCourtRequest.copy(courtTypeCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ court type not found for court ${createCourtRequest.courtId}")
      }

      @Test
      fun `court name is blank`() {
        webTestClient.post()
          .uri("/courts")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(createCourtRequest.copy(courtName = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will persist the court, address, email address and phone number`() {
        webTestClient.post()
          .uri("/courts")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .bodyValue(createCourtRequest)
          .exchange()
          .expectStatus().isCreated

        transactionHelper.runInTransaction {
          val persistedCourt = courtRepository.findByIdOrNull(createCourtRequest.courtId)

          assertThat(persistedCourt).isNotNull
          assertThat(persistedCourt!!.name).isEqualTo("New Court")
          assertThat(persistedCourt.description).isEqualTo("The New Court")
          assertThat(persistedCourt.active).isTrue
          assertThat(persistedCourt.cjitCode).isEqualTo("123456789")
          assertThat(persistedCourt.accessibleAccess).isEqualTo(AccessibleAccess.ACCESSIBLE)
          assertThat(persistedCourt.area?.code).isEqualTo("52")
          assertThat(persistedCourt.region?.code).isEqualTo("YOHUM")
          assertThat(persistedCourt.geographicalArea?.code).isEqualTo("WYORKS")
          assertThat(persistedCourt.localAuthority?.code).isEqualTo("00CG")
          assertThat(persistedCourt.payrollRegion?.code).isEqualTo("NEY")
          assertThat(persistedCourt.courtType.code).isEqualTo("CC")

          assertThat(persistedCourt.addresses).hasSize(1)
          assertThat(persistedCourt.addresses[0].addressLine1).isEqualTo("Court House, 31 High Street")
          assertThat(persistedCourt.addresses[0].addressLine2).isEqualTo("City Centre")
          assertThat(persistedCourt.addresses[0].town).isEqualTo("Sheffield")
          assertThat(persistedCourt.addresses[0].county).isEqualTo("South Yorkshire")
          assertThat(persistedCourt.addresses[0].postcode).isEqualTo("S1 3GG")
          assertThat(persistedCourt.addresses[0].country).isEqualTo("England")

          assertThat(persistedCourt.emailAddresses).hasSize(1)
          assertThat(persistedCourt.emailAddresses[0].value).isEqualTo("test@justice.gov.uk")

          assertThat(persistedCourt.phoneNumbers).hasSize(2)
          assertThat(persistedCourt.phoneNumbers[0].value).isEqualTo("0114 555 8989")
          assertThat(persistedCourt.phoneNumbers[1].value).isEqualTo("0114 555 7777")
        }
      }
    }
  }
}
