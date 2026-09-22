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
import uk.gov.justice.digital.hmpps.prisonregister.model.Agency
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumberRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class AgencyResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var agencyRepository: AgencyRepository

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
        assertThat(agencyDto.localAuthority?.description).isEqualTo("Sheffield City Council")
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

  @DisplayName("Get all agencies")
  @Nested
  inner class GetAll {
    lateinit var agency: Agency
    lateinit var agency2: Agency
    lateinit var agency3: Agency

    @BeforeEach
    fun setUp() {
      agency = dsl.agency(
        agencyId = "SHEFCC",
        name = "Sheffield Crown Court",
        description = "Sheffield Crown Court City Centre",
        active = false,
        agencyType = AgencyType.PROBATION_CRC,
        inactiveDate = LocalDate.parse("2020-01-02"),
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {}

      agency2 = dsl.agency(
        agencyId = "LEEDCC",
        name = "Leeds Crown Court",
        agencyType = AgencyType.AIRPORT,
      ) {}

      agency3 = dsl.agency(
        agencyId = "BIRMMC",
        name = "Birmingham Magistrates Court",
        agencyType = AgencyType.YOT,
      ) {}
    }

    @AfterEach
    fun tearDown() {
      agencyRepository.deleteById(agency.agencyId)
      agencyRepository.deleteById(agency2.agencyId)
      agencyRepository.deleteById(agency3.agencyId)
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return all agencies`() {
        val agencies = webTestClient.get()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyList(AgencyDto::class.java)
          .returnResult()
          .responseBody!!

        assertThat(agencies).extracting("agencyId").contains("SHEFCC", "LEEDCC", "BIRMMC")

        val sheffieldAgency = agencies.first { it.agencyId == "SHEFCC" }
        assertThat(sheffieldAgency.agencyName).isEqualTo("Sheffield Crown Court")
        assertThat(sheffieldAgency.description).isEqualTo("Sheffield Crown Court City Centre")
        assertThat(sheffieldAgency.active).isFalse
        assertThat(sheffieldAgency.agencyType).isEqualTo("PROBATION_CRC")

        val leedsAgency = agencies.first { it.agencyId == "LEEDCC" }
        assertThat(leedsAgency.agencyName).isEqualTo("Leeds Crown Court")
        assertThat(leedsAgency.agencyType).isEqualTo("AIRPORT")

        val birminghamAgency = agencies.first { it.agencyId == "BIRMMC" }
        assertThat(birminghamAgency.agencyName).isEqualTo("Birmingham Magistrates Court")
        assertThat(birminghamAgency.agencyType).isEqualTo("YOT")
      }
    }
  }

  @DisplayName("Create agency")
  @Nested
  inner class CreateAgency {
    val createAgencyRequest = CreateAgencyDto(
      agencyId = "NEWAIR",
      agencyName = "New Airport Agency",
      description = "The New Airport Agency",
      active = true,
      accessibleAccess = AccessibleAccess.BY_ARRANGEMENT_ONLY,
      agencyType = AgencyType.AIRPORT,
      inactiveDate = null,
      cjitCode = "123456789",
      areaCode = "52",
      regionCode = "YOHUM",
      geographicalAreaCode = "WYORKS",
      localAuthorityCode = "00CG",
      payrollRegionCode = "NEY",
      addresses = listOf(
        UpdateAddressDto(
          addressLine1 = "Airport House, 31 High Street",
          addressLine2 = "City Centre",
          town = "Sheffield",
          county = "South Yorkshire",
          postcode = "S1 3GG",
          country = "England",
        ),
      ),
      emailAddresses = listOf(
        UpdateEmailAddressDto(address = "newairport@justice.gov.uk"),
      ),
      phoneNumbers = listOf(
        UpdatePhoneNumberDto(number = "0114 555 8989"),
        UpdatePhoneNumberDto(number = "0114 555 7777"),
      ),
    )

    @AfterEach
    fun tearDown() {
      agencyRepository.findByIdOrNull(createAgencyRequest.agencyId)?.let { agencyRepository.delete(it) }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createAgencyRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createAgencyRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAgencyRequest)
          .exchange()
          .expectStatus().isCreated
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `agency id already exists`() {
        dsl.agency(agencyId = createAgencyRequest.agencyId, name = "Existing Agency") {}

        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAgencyRequest)
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("Agency ${createAgencyRequest.agencyId} already exists")
      }

      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAgencyRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency ${createAgencyRequest.agencyId}")
      }

      @Test
      fun `region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAgencyRequest.copy(regionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency ${createAgencyRequest.agencyId}")
      }

      @Test
      fun `geographical area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAgencyRequest.copy(geographicalAreaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency ${createAgencyRequest.agencyId}")
      }

      @Test
      fun `local authority code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAgencyRequest.copy(localAuthorityCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for agency ${createAgencyRequest.agencyId}")
      }

      @Test
      fun `payroll region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAgencyRequest.copy(payrollRegionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for agency ${createAgencyRequest.agencyId}")
      }

      @Test
      fun `agency name is blank`() {
        webTestClient.post()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAgencyRequest.copy(agencyName = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will persist the agency, address, email address and phone number`() {
        val dto: AgencyDto = webTestClient.post()
          .uri("/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAgencyRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(dto.agencyId).isEqualTo("NEWAIR")
        assertThat(dto.agencyType).isEqualTo("AIRPORT")

        transactionHelper.runInTransaction {
          val persisted = agencyRepository.findByIdOrNull(createAgencyRequest.agencyId)

          assertThat(persisted).isNotNull
          assertThat(persisted!!.name).isEqualTo("New Airport Agency")
          assertThat(persisted.description).isEqualTo("The New Airport Agency")
          assertThat(persisted.active).isTrue
          assertThat(persisted.accessibleAccess).isEqualTo(AccessibleAccess.BY_ARRANGEMENT_ONLY)
          assertThat(persisted.agencyType).isEqualTo(AgencyType.AIRPORT)
          assertThat(persisted.cjitCode).isEqualTo("123456789")
          assertThat(persisted.area?.code).isEqualTo("52")
          assertThat(persisted.region?.code).isEqualTo("YOHUM")
          assertThat(persisted.geographicalArea?.code).isEqualTo("WYORKS")
          assertThat(persisted.localAuthority?.code).isEqualTo("00CG")
          assertThat(persisted.payrollRegion?.code).isEqualTo("NEY")

          assertThat(persisted.addresses).hasSize(1)
          assertThat(persisted.addresses[0].addressLine1).isEqualTo("Airport House, 31 High Street")
          assertThat(persisted.addresses[0].addressLine2).isEqualTo("City Centre")
          assertThat(persisted.addresses[0].town).isEqualTo("Sheffield")
          assertThat(persisted.addresses[0].county).isEqualTo("South Yorkshire")
          assertThat(persisted.addresses[0].postcode).isEqualTo("S1 3GG")
          assertThat(persisted.addresses[0].country).isEqualTo("England")

          assertThat(persisted.emailAddresses).hasSize(1)
          assertThat(persisted.emailAddresses[0].value).isEqualTo("newairport@justice.gov.uk")

          assertThat(persisted.phoneNumbers).hasSize(2)
          assertThat(persisted.phoneNumbers.map { it.value }).containsExactlyInAnyOrder("0114 555 8989", "0114 555 7777")
        }

        verify(telemetryClient).trackEvent(
          eq("agency-created"),
          check {
            assertThat(it["agencyId"]).isEqualTo(createAgencyRequest.agencyId)
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Create agency address")
  @Nested
  inner class CreateAgencyAddress {
    lateinit var agency: Agency

    val createAddressRequest = UpdateAddressDto(
      addressLine1 = "Court House, 31 High Street",
      addressLine2 = "City Centre",
      town = "Sheffield",
      county = "South Yorkshire",
      postcode = "S1 3GG",
      country = "England",
    )

    @BeforeEach
    fun setUp() {
      agency = dsl.agency(
        agencyId = "SHEFCC",
        name = "Sheffield Crown Court",
        description = "Sheffield Crown Court City Centre",
        active = true,
        agencyType = AgencyType.PROBATION_CRC,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Existing Crown Court House",
          town = "Leeds",
          postcode = "LS1 1AA",
          country = "England",
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
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/address")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/address")
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
      fun `404 if agency not found`() {
        webTestClient.post()
          .uri("/agencies/id/ZZZZ/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if town is missing`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(mapOf("postcode" to "S1 3GG", "country" to "England"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if postcode is too long`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/address")
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
      fun `will persist the new address against the agency`() {
        val addressDto: AgencyAddressDto = webTestClient.post()
          .uri("/agencies/id/SHEFCC/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(addressDto.addressLine1).isEqualTo("Court House, 31 High Street")
        assertThat(addressDto.addressLine2).isEqualTo("City Centre")
        assertThat(addressDto.town).isEqualTo("Sheffield")
        assertThat(addressDto.county).isEqualTo("South Yorkshire")
        assertThat(addressDto.postcode).isEqualTo("S1 3GG")
        assertThat(addressDto.country).isEqualTo("England")
        assertThat(addressDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persisted = agencyRepository.findByIdOrNull("SHEFCC")!!
          assertThat(persisted.addresses).hasSize(2)
          val persistedAddress = persisted.addresses.find { it.id == addressDto.id }
          assertThat(persistedAddress).isNotNull
          assertThat(persistedAddress!!.addressLine1).isEqualTo("Court House, 31 High Street")
        }

        verify(telemetryClient).trackEvent(
          eq("agency-address-created"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
            assertThat(it["addressId"]).isEqualTo(addressDto.id.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Create agency phone number")
  @Nested
  inner class CreateAgencyPhoneNumber {
    lateinit var agency: Agency

    val createPhoneNumberRequest = UpdatePhoneNumberDto(number = "0114 555 8989")

    @BeforeEach
    fun setUp() {
      agency = dsl.agency(
        agencyId = "SHEFCC",
        name = "Sheffield Crown Court",
        description = "Sheffield Crown Court City Centre",
        active = true,
        agencyType = AgencyType.PROBATION_CRC,
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
      if (::agency.isInitialized) {
        agencyRepository.deleteById(agency.agencyId)
      }
      agencyRepository.findByIdOrNull("OTHAG")?.let { agencyRepository.deleteById("OTHAG") }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/phone-number")
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
      fun `404 if agency not found`() {
        webTestClient.post()
          .uri("/agencies/id/ZZZZ/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if phone number is in an incorrect format`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest.copy(number = "not-a-number"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if phone number is blank`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest.copy(number = ""))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `409 if phone number already exists on this agency`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated

        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/agencies/id/SHEFCC/phone-number")
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
      fun `will allow the same phone number to be used by a different agency`() {
        dsl.agency(agencyId = "OTHAG", name = "Other Agency", agencyType = AgencyType.PECS) {}

        val phoneDto: AgencyPhoneDto = webTestClient.post()
          .uri("/agencies/id/OTHAG/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        verify(telemetryClient).trackEvent(
          eq("agency-phone-number-created"),
          check {
            assertThat(it["agencyId"]).isEqualTo("OTHAG")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneDto.id.toString())
          },
          isNull(),
        )
      }

      @Test
      fun `will persist the new phone number against the agency`() {
        val phoneDto: AgencyPhoneDto = webTestClient.post()
          .uri("/agencies/id/SHEFCC/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(phoneDto.number).isEqualTo("0114 555 8989")
        assertThat(phoneDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persisted = agencyRepository.findByIdOrNull("SHEFCC")!!
          assertThat(persisted.phoneNumbers).hasSize(2)
          val persistedPhoneNumber = persisted.phoneNumbers.find { it.id == phoneDto.id }
          assertThat(persistedPhoneNumber).isNotNull
          assertThat(persistedPhoneNumber!!.value).isEqualTo("0114 555 8989")
        }

        verify(telemetryClient).trackEvent(
          eq("agency-phone-number-created"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneDto.id.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Create agency email address")
  @Nested
  inner class CreateAgencyEmailAddress {
    lateinit var agency: Agency

    val createEmailAddressRequest = UpdateEmailAddressDto(address = "newagency@justice.gov.uk")

    @BeforeEach
    fun setUp() {
      agency = dsl.agency(
        agencyId = "SHEFCC",
        name = "Sheffield Crown Court",
        description = "Sheffield Crown Court City Centre",
        active = true,
        agencyType = AgencyType.PROBATION_CRC,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        email(
          emailAddress = "existingagency@justice.gov.uk",
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
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/email-address")
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
      fun `404 if agency not found`() {
        webTestClient.post()
          .uri("/agencies/id/ZZZZ/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if email address is in an incorrect format`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest.copy(address = "not-an-email"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if email address is blank`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest.copy(address = ""))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `409 if email address already exists`() {
        webTestClient.post()
          .uri("/agencies/id/SHEFCC/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isCreated

        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/agencies/id/SHEFCC/email-address")
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
      fun `will persist the new email address against the agency`() {
        val emailDto: AgencyEmailDto = webTestClient.post()
          .uri("/agencies/id/SHEFCC/email-address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createEmailAddressRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(emailDto.address).isEqualTo("newagency@justice.gov.uk")
        assertThat(emailDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persisted = agencyRepository.findByIdOrNull("SHEFCC")!!
          assertThat(persisted.emailAddresses).hasSize(2)
          val persistedEmailAddress = persisted.emailAddresses.find { it.id == emailDto.id }
          assertThat(persistedEmailAddress).isNotNull
          assertThat(persistedEmailAddress!!.value).isEqualTo("newagency@justice.gov.uk")
        }

        verify(telemetryClient).trackEvent(
          eq("agency-email-address-created"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
            assertThat(it["emailAddressId"]).isEqualTo(emailDto.id.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Update agency")
  @Nested
  inner class UpdateAgency {
    lateinit var agency: Agency

    val updateAgencyRequest = UpdateAgencyDto(
      agencyName = "Sheffield Transport Agency",
      description = "Sheffield City Transport Agency",
      active = true,
      accessibleAccess = AccessibleAccess.WHEELCHAIR_ACCESS,
      agencyType = AgencyType.PECS,
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
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Crown Court, 1 Bank Street",
          town = "Sheffield",
          postcode = "S1 2DS",
          country = "England",
        )
        email(
          emailAddress = "sheffield@justice.gov.uk",
        )
        phoneNumber(
          phoneNumber = "0114 555 1234",
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
        webTestClient.put()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateAgencyRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateAgencyRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAgencyRequest)
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if not found`() {
        webTestClient.put()
          .uri("/agencies/id/ZZZZ")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAgencyRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAgencyRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency SHEFCC")
      }

      @Test
      fun `region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAgencyRequest.copy(regionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency SHEFCC")
      }

      @Test
      fun `geographical area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAgencyRequest.copy(geographicalAreaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SHEFCC")
      }

      @Test
      fun `local authority code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAgencyRequest.copy(localAuthorityCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for agency SHEFCC")
      }

      @Test
      fun `payroll region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAgencyRequest.copy(payrollRegionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for agency SHEFCC")
      }

      @Test
      fun `agency name is blank`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAgencyRequest.copy(agencyName = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the core agency data`() {
        val dto: AgencyDto = webTestClient.put()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAgencyRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(dto.agencyId).isEqualTo("SHEFCC")
        assertThat(dto.agencyName).isEqualTo("Sheffield Transport Agency")
        assertThat(dto.description).isEqualTo("Sheffield City Transport Agency")
        assertThat(dto.active).isFalse
        assertThat(dto.accessibleAccess).isEqualTo("WHEELCHAIR_ACCESS")
        assertThat(dto.agencyType).isEqualTo("PECS")
        assertThat(dto.inactiveDate).isEqualTo("2026-01-01")
        assertThat(dto.cjitCode).isEqualTo("123456789")
        assertThat(dto.area?.description).isEqualTo("South Yorkshire")
        assertThat(dto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(dto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(dto.localAuthority?.description).isEqualTo("Sheffield City Council")
        assertThat(dto.payrollRegion?.code).isEqualTo("NEY")

        verify(telemetryClient).trackEvent(
          eq("agency-updated"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
          },
          isNull(),
        )
      }

      @Test
      fun `will not affect addresses, emails or phone numbers`() {
        val dto: AgencyDto = webTestClient.put()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAgencyRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(dto.addresses).hasSize(1)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("Crown Court, 1 Bank Street")
        assertThat(dto.emailAddresses).hasSize(1)
        assertThat(dto.emailAddresses[0].address).isEqualTo("sheffield@justice.gov.uk")
        assertThat(dto.phoneNumbers).hasSize(1)
        assertThat(dto.phoneNumbers[0].number).isEqualTo("0114 555 1234")

        verify(telemetryClient).trackEvent(
          eq("agency-updated"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Update agency address")
  @Nested
  inner class UpdateAgencyAddress {
    lateinit var agency: Agency
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
      agency = dsl.agency(
        agencyId = "SHEFCC",
        name = "Sheffield Crown Court",
        description = "Sheffield Crown Court City Centre",
        active = true,
        agencyType = AgencyType.PROBATION_CRC,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Crown Court, 1 Bank Street",
          town = "Sheffield",
          postcode = "S1 2DS",
          country = "England",
        )
      }
      addressId = agency.addresses[0].id
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
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/address/{addressId}", addressId)
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
      fun `404 if agency not found`() {
        webTestClient.put()
          .uri("/agencies/id/ZZZZ/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if address not found`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/address/{addressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if town is missing`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(mapOf("postcode" to "S1 3GG", "country" to "England"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if postcode is too long`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/address/{addressId}", addressId)
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
          .uri("/agencies/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
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

        verify(telemetryClient).trackEvent(
          eq("agency-address-updated"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
            assertThat(it["addressId"]).isEqualTo(addressId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Update agency phone number")
  @Nested
  inner class UpdateAgencyPhoneNumber {
    lateinit var agency: Agency
    var phoneNumberId: Long = -1

    val updatePhoneNumberRequest = UpdatePhoneNumberDto(number = "0114 555 1234")

    @BeforeEach
    fun setUp() {
      agency = dsl.agency(
        agencyId = "SHEFCC",
        name = "Sheffield Crown Court",
        description = "Sheffield Crown Court City Centre",
        active = true,
        agencyType = AgencyType.PROBATION_CRC,
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
      phoneNumberId = agency.phoneNumbers.first { it.value == "0114 555 8989" }.id
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
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
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
      fun `404 if agency not found`() {
        webTestClient.put()
          .uri("/agencies/id/ZZZZ/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if phone number not found`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if phone number is in an incorrect format`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = "not-a-number"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if phone number is blank`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = ""))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `409 if phone number already exists on this agency`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
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
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(phoneDto.id).isEqualTo(phoneNumberId)
        assertThat(phoneDto.number).isEqualTo("0114 555 1234")

        verify(telemetryClient).trackEvent(
          eq("agency-phone-number-updated"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneNumberId.toString())
          },
          isNull(),
        )
      }

      @Test
      fun `will allow updating a phone number to its own current value`() {
        val phoneDto: AgencyPhoneDto = webTestClient.put()
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = "0114 555 8989"))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(phoneDto.number).isEqualTo("0114 555 8989")

        verify(telemetryClient).trackEvent(
          eq("agency-phone-number-updated"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneNumberId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Update agency email address")
  @Nested
  inner class UpdateAgencyEmailAddress {
    lateinit var agency: Agency
    var emailAddressId: Long = -1

    val updateEmailAddressRequest = UpdateEmailAddressDto(address = "updatedagency@justice.gov.uk")

    @BeforeEach
    fun setUp() {
      agency = dsl.agency(
        agencyId = "SHEFCC",
        name = "Sheffield Crown Court",
        description = "Sheffield Crown Court City Centre",
        active = true,
        agencyType = AgencyType.PROBATION_CRC,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        email(
          emailAddress = "sheffield-update@justice.gov.uk",
        )
      }
      emailAddressId = agency.emailAddresses[0].id
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
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
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
      fun `404 if agency not found`() {
        webTestClient.put()
          .uri("/agencies/id/ZZZZ/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if email address not found`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/email-address/{emailAddressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if email address is in an incorrect format`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest.copy(address = "not-an-email"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if email address is blank`() {
        webTestClient.put()
          .uri("/agencies/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
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
          .uri("/agencies/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateEmailAddressRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(emailDto.id).isEqualTo(emailAddressId)
        assertThat(emailDto.address).isEqualTo("updatedagency@justice.gov.uk")

        verify(telemetryClient).trackEvent(
          eq("agency-email-address-updated"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
            assertThat(it["emailAddressId"]).isEqualTo(emailAddressId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Delete agency")
  @Nested
  inner class DeleteAgency {
    lateinit var agency: Agency

    @BeforeEach
    fun setUp() {
      agency = dsl.agency(
        agencyId = "SHEFCC",
        name = "Sheffield Crown Court",
        description = "Sheffield Crown Court City Centre",
        active = true,
        agencyType = AgencyType.PROBATION_CRC,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Crown Court, 1 Bank Street",
          town = "Sheffield",
          postcode = "S1 2DS",
          country = "England",
        )
        email(
          emailAddress = "sheffield-delete@justice.gov.uk",
        )
        phoneNumber(
          phoneNumber = "0114 555 8989",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      if (::agency.isInitialized) {
        agencyRepository.findByIdOrNull(agency.agencyId)?.let { agencyRepository.deleteById(agency.agencyId) }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/agencies/id/SHEFCC")
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
      fun `will delete the agency, along with its addresses, emails and phone numbers`() {
        val addressId = agency.addresses[0].id
        val emailAddressId = agency.emailAddresses[0].id
        val phoneNumberId = agency.phoneNumbers[0].id

        webTestClient.delete()
          .uri("/agencies/id/SHEFCC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(agencyRepository.findByIdOrNull("SHEFCC")).isNull()
          assertThat(agencyAddressRepository.findByIdOrNull(addressId)).isNull()
          assertThat(emailAddressRepository.findByIdOrNull(emailAddressId)).isNull()
          assertThat(phoneNumberRepository.findByIdOrNull(phoneNumberId)).isNull()
        }

        verify(telemetryClient).trackEvent(
          eq("agency-deleted"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Delete agency address")
  @Nested
  inner class DeleteAgencyAddress {
    lateinit var agency: Agency
    var addressId: Long = -1

    @BeforeEach
    fun setUp() {
      agency = dsl.agency(
        agencyId = "SHEFCC",
        name = "Sheffield Crown Court",
        description = "Sheffield Crown Court City Centre",
        active = true,
        agencyType = AgencyType.PROBATION_CRC,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        address(
          addressLine1 = "Crown Court, 1 Bank Street",
          town = "Sheffield",
          postcode = "S1 2DS",
          country = "England",
        )
      }
      addressId = agency.addresses[0].id
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
        webTestClient.delete()
          .uri("/agencies/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/agencies/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if agency not found`() {
        webTestClient.delete()
          .uri("/agencies/id/ZZZZ/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if address not found`() {
        webTestClient.delete()
          .uri("/agencies/id/SHEFCC/address/{addressId}", 999999)
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
          .uri("/agencies/id/SHEFCC/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(agencyAddressRepository.findByIdOrNull(addressId)).isNull()
          assertThat(agencyRepository.findByIdOrNull("SHEFCC")?.addresses).isEmpty()
        }

        verify(telemetryClient).trackEvent(
          eq("agency-address-deleted"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
            assertThat(it["addressId"]).isEqualTo(addressId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Delete agency phone number")
  @Nested
  inner class DeleteAgencyPhoneNumber {
    lateinit var agency: Agency
    var phoneNumberId: Long = -1

    @BeforeEach
    fun setUp() {
      agency = dsl.agency(
        agencyId = "SHEFCC",
        name = "Sheffield Crown Court",
        description = "Sheffield Crown Court City Centre",
        active = true,
        agencyType = AgencyType.PROBATION_CRC,
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
      phoneNumberId = agency.phoneNumbers[0].id
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
        webTestClient.delete()
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if agency not found`() {
        webTestClient.delete()
          .uri("/agencies/id/ZZZZ/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if phone number not found`() {
        webTestClient.delete()
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", 999999)
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
          .uri("/agencies/id/SHEFCC/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(phoneNumberRepository.findByIdOrNull(phoneNumberId)).isNull()
          assertThat(agencyRepository.findByIdOrNull("SHEFCC")?.phoneNumbers).isEmpty()
        }

        verify(telemetryClient).trackEvent(
          eq("agency-phone-number-deleted"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneNumberId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Delete agency email address")
  @Nested
  inner class DeleteAgencyEmailAddress {
    lateinit var agency: Agency
    var emailAddressId: Long = -1

    @BeforeEach
    fun setUp() {
      agency = dsl.agency(
        agencyId = "SHEFCC",
        name = "Sheffield Crown Court",
        description = "Sheffield Crown Court City Centre",
        active = true,
        agencyType = AgencyType.PROBATION_CRC,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
      ) {
        email(
          emailAddress = "sheffield-delete-email@justice.gov.uk",
        )
      }
      emailAddressId = agency.emailAddresses[0].id
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
        webTestClient.delete()
          .uri("/agencies/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/agencies/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if agency not found`() {
        webTestClient.delete()
          .uri("/agencies/id/ZZZZ/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if email address not found`() {
        webTestClient.delete()
          .uri("/agencies/id/SHEFCC/email-address/{emailAddressId}", 999999)
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
          .uri("/agencies/id/SHEFCC/email-address/{emailAddressId}", emailAddressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(emailAddressRepository.findByIdOrNull(emailAddressId)).isNull()
          assertThat(agencyRepository.findByIdOrNull("SHEFCC")?.emailAddresses).isEmpty()
        }

        verify(telemetryClient).trackEvent(
          eq("agency-email-address-deleted"),
          check {
            assertThat(it["agencyId"]).isEqualTo("SHEFCC")
            assertThat(it["emailAddressId"]).isEqualTo(emailAddressId.toString())
          },
          isNull(),
        )
      }
    }
  }
}
