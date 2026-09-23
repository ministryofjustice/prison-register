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
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Hospital
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumberRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class HospitalResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var hospitalRepository: HospitalRepository

  @Autowired
  lateinit var agencyAddressRepository: AgencyAddressRepository

  @Autowired
  lateinit var phoneNumberRepository: PhoneNumberRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @MockitoBean
  private lateinit var telemetryClient: TelemetryClient

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
        localAuthorityCode = "00CG",
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
        assertThat(hospitalDto.localAuthority?.description).isEqualTo("Sheffield City Council")
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

  @DisplayName("Get all hospitals")
  @Nested
  inner class GetAll {
    lateinit var hospital: Hospital
    lateinit var hospital2: Hospital
    lateinit var hospital3: Hospital

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
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "HS",
      ) {}

      hospital2 = dsl.hospital(
        hospitalId = "LEEDHO",
        name = "Leeds Hospital",
      ) {}

      hospital3 = dsl.hospital(
        hospitalId = "BIRMHO",
        name = "Birmingham Hospital",
      ) {}
    }

    @AfterEach
    fun tearDown() {
      hospitalRepository.deleteById(hospital.hospitalId)
      hospitalRepository.deleteById(hospital2.hospitalId)
      hospitalRepository.deleteById(hospital3.hospitalId)
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/hospitals")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/hospitals")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/hospitals")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return all hospitals`() {
        val hospitals = webTestClient.get()
          .uri("/hospitals")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyList(HospitalDto::class.java)
          .returnResult()
          .responseBody!!

        assertThat(hospitals).extracting("hospitalId").contains("SHFHOS", "LEEDHO", "BIRMHO")

        val hospitalDto = hospitals.first { it.hospitalId == "SHFHOS" }
        assertThat(hospitalDto.hospitalName).isEqualTo("Sheffield Secure Hospital")
        assertThat(hospitalDto.description).isEqualTo("Sheffield Central Secure Hospital")
        assertThat(hospitalDto.active).isFalse

        val hospital2Dto = hospitals.first { it.hospitalId == "LEEDHO" }
        assertThat(hospital2Dto.hospitalName).isEqualTo("Leeds Hospital")

        val hospital3Dto = hospitals.first { it.hospitalId == "BIRMHO" }
        assertThat(hospital3Dto.hospitalName).isEqualTo("Birmingham Hospital")
      }
    }
  }

  @DisplayName("Create hospital")
  @Nested
  inner class CreateHospital {
    val createHospitalRequest = CreateHospitalDto(
      hospitalId = "NEWHOS",
      hospitalName = "New Hospital",
      description = "The New Hospital",
      active = true,
      inactiveDate = null,
      cjitCode = "123456789",
      areaCode = "52",
      regionCode = "YOHUM",
      geographicalAreaCode = "WYORKS",
      localAuthorityCode = "00CG",
      payrollRegionCode = "NEY",
      highSecurity = false,
      addresses = listOf(
        UpdateAddressDto(
          addressLine1 = "Hospital House, 31 High Street",
          addressLine2 = "City Centre",
          town = "Sheffield",
          county = "South Yorkshire",
          postcode = "S1 3GG",
          country = "England",
        ),
      ),
      phoneNumbers = listOf(
        UpdatePhoneNumberDto(number = "0114 555 8989"),
        UpdatePhoneNumberDto(number = "0114 555 7777"),
      ),
    )

    @AfterEach
    fun tearDown() {
      hospitalRepository.findByIdOrNull(createHospitalRequest.hospitalId)?.let { hospitalRepository.delete(it) }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/hospitals")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createHospitalRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/hospitals")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createHospitalRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/hospitals")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createHospitalRequest)
          .exchange()
          .expectStatus().isCreated
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `hospital id already exists`() {
        dsl.hospital(hospitalId = createHospitalRequest.hospitalId, name = "Existing Hospital") {}

        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/hospitals")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createHospitalRequest)
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("Hospital ${createHospitalRequest.hospitalId} already exists")
      }

      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.post()
          .uri("/hospitals")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createHospitalRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for hospital ${createHospitalRequest.hospitalId}")
      }

      @Test
      fun `hospital name is blank`() {
        webTestClient.post()
          .uri("/hospitals")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createHospitalRequest.copy(hospitalName = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will persist the hospital, address and phone numbers`() {
        webTestClient.post()
          .uri("/hospitals")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createHospitalRequest)
          .exchange()
          .expectStatus().isCreated

        transactionHelper.runInTransaction {
          val persistedHospital = hospitalRepository.findByIdOrNull(createHospitalRequest.hospitalId)

          assertThat(persistedHospital).isNotNull
          assertThat(persistedHospital!!.name).isEqualTo("New Hospital")
          assertThat(persistedHospital.description).isEqualTo("The New Hospital")
          assertThat(persistedHospital.active).isTrue
          assertThat(persistedHospital.highSecurity).isFalse
          assertThat(persistedHospital.cjitCode).isEqualTo("123456789")
          assertThat(persistedHospital.area?.code).isEqualTo("52")
          assertThat(persistedHospital.region?.code).isEqualTo("YOHUM")
          assertThat(persistedHospital.geographicalArea?.code).isEqualTo("WYORKS")
          assertThat(persistedHospital.localAuthority?.code).isEqualTo("00CG")
          assertThat(persistedHospital.payrollRegion?.code).isEqualTo("NEY")

          assertThat(persistedHospital.addresses).hasSize(1)
          assertThat(persistedHospital.addresses[0].addressLine1).isEqualTo("Hospital House, 31 High Street")
          assertThat(persistedHospital.addresses[0].addressLine2).isEqualTo("City Centre")
          assertThat(persistedHospital.addresses[0].town).isEqualTo("Sheffield")
          assertThat(persistedHospital.addresses[0].county).isEqualTo("South Yorkshire")
          assertThat(persistedHospital.addresses[0].postcode).isEqualTo("S1 3GG")
          assertThat(persistedHospital.addresses[0].country).isEqualTo("England")

          assertThat(persistedHospital.phoneNumbers).hasSize(2)
          assertThat(persistedHospital.phoneNumbers.map { it.value }).containsExactlyInAnyOrder("0114 555 8989", "0114 555 7777")
        }

        verify(telemetryClient).trackEvent(
          eq("hospital-created"),
          check {
            assertThat(it["hospitalId"]).isEqualTo(createHospitalRequest.hospitalId)
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Create hospital address")
  @Nested
  inner class CreateHospitalAddress {
    lateinit var hospital: Hospital

    val createAddressRequest = UpdateAddressDto(
      addressLine1 = "Hospital House, 31 High Street",
      addressLine2 = "City Centre",
      town = "Sheffield",
      county = "South Yorkshire",
      postcode = "S1 3GG",
      country = "England",
    )

    @BeforeEach
    fun setUp() {
      hospital = dsl.hospital(
        hospitalId = "SHFHOS",
        name = "Sheffield Secure Hospital",
        description = "Sheffield Central Secure Hospital",
        active = true,
        highSecurity = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "HS",
      ) {
        address(
          addressLine1 = "Existing Hospital House",
          town = "Leeds",
          postcode = "LS1 1AA",
          country = "England",
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
        webTestClient.post()
          .uri("/hospitals/id/SHFHOS/address")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/hospitals/id/SHFHOS/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/hospitals/id/SHFHOS/address")
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
      fun `404 if hospital not found`() {
        webTestClient.post()
          .uri("/hospitals/id/ZZZZ/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if town is missing`() {
        webTestClient.post()
          .uri("/hospitals/id/SHFHOS/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(mapOf("postcode" to "S1 3GG", "country" to "England"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if postcode is too long`() {
        webTestClient.post()
          .uri("/hospitals/id/SHFHOS/address")
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
      fun `will persist the new address against the hospital`() {
        val addressDto: AgencyAddressDto = webTestClient.post()
          .uri("/hospitals/id/SHFHOS/address")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createAddressRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(addressDto.addressLine1).isEqualTo("Hospital House, 31 High Street")
        assertThat(addressDto.addressLine2).isEqualTo("City Centre")
        assertThat(addressDto.town).isEqualTo("Sheffield")
        assertThat(addressDto.county).isEqualTo("South Yorkshire")
        assertThat(addressDto.postcode).isEqualTo("S1 3GG")
        assertThat(addressDto.country).isEqualTo("England")
        assertThat(addressDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persistedHospital = hospitalRepository.findByIdOrNull("SHFHOS")!!
          assertThat(persistedHospital.addresses).hasSize(2)
          val persistedAddress = persistedHospital.addresses.find { it.id == addressDto.id }
          assertThat(persistedAddress).isNotNull
          assertThat(persistedAddress!!.addressLine1).isEqualTo("Hospital House, 31 High Street")
        }

        verify(telemetryClient).trackEvent(
          eq("hospital-address-created"),
          check {
            assertThat(it["hospitalId"]).isEqualTo("SHFHOS")
            assertThat(it["addressId"]).isEqualTo(addressDto.id.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Create hospital phone number")
  @Nested
  inner class CreateHospitalPhoneNumber {
    lateinit var hospital: Hospital

    val createPhoneNumberRequest = UpdatePhoneNumberDto(number = "0114 555 8989")

    @BeforeEach
    fun setUp() {
      hospital = dsl.hospital(
        hospitalId = "SHFHOS",
        name = "Sheffield Secure Hospital",
        description = "Sheffield Central Secure Hospital",
        active = true,
        highSecurity = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "HS",
      ) {
        phoneNumber(
          phoneNumber = "0114 555 1111",
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
        webTestClient.post()
          .uri("/hospitals/id/SHFHOS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/hospitals/id/SHFHOS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/hospitals/id/SHFHOS/phone-number")
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
      fun `404 if hospital not found`() {
        webTestClient.post()
          .uri("/hospitals/id/ZZZZ/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if phone number is in an incorrect format`() {
        webTestClient.post()
          .uri("/hospitals/id/SHFHOS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest.copy(number = "not-a-number"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if phone number is blank`() {
        webTestClient.post()
          .uri("/hospitals/id/SHFHOS/phone-number")
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
      fun `will allow the same phone number to be used by a different hospital`() {
        dsl.hospital(hospitalId = "OTHHOS", name = "Other Hospital") {}

        val phoneDto: AgencyPhoneDto = webTestClient.post()
          .uri("/hospitals/id/OTHHOS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        verify(telemetryClient).trackEvent(
          eq("hospital-phone-number-created"),
          check {
            assertThat(it["hospitalId"]).isEqualTo("OTHHOS")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneDto.id.toString())
          },
          isNull(),
        )

        hospitalRepository.deleteById("OTHHOS")
      }

      @Test
      fun `will persist the new phone number against the hospital`() {
        val phoneDto: AgencyPhoneDto = webTestClient.post()
          .uri("/hospitals/id/SHFHOS/phone-number")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(createPhoneNumberRequest)
          .exchange()
          .expectStatus().isCreated.expectBodyResponse()

        assertThat(phoneDto.number).isEqualTo("0114 555 8989")
        assertThat(phoneDto.id).isNotEqualTo(-1)

        transactionHelper.runInTransaction {
          val persistedHospital = hospitalRepository.findByIdOrNull("SHFHOS")!!
          assertThat(persistedHospital.phoneNumbers).hasSize(2)
          val persistedPhoneNumber = persistedHospital.phoneNumbers.find { it.id == phoneDto.id }
          assertThat(persistedPhoneNumber).isNotNull
          assertThat(persistedPhoneNumber!!.value).isEqualTo("0114 555 8989")
        }

        verify(telemetryClient).trackEvent(
          eq("hospital-phone-number-created"),
          check {
            assertThat(it["hospitalId"]).isEqualTo("SHFHOS")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneDto.id.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Delete hospital")
  @Nested
  inner class DeleteHospital {
    lateinit var hospital: Hospital

    @BeforeEach
    fun setUp() {
      hospital = dsl.hospital(
        hospitalId = "SHFHOS",
        name = "Sheffield Secure Hospital",
        description = "Sheffield Central Secure Hospital",
        active = true,
        highSecurity = true,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "HS",
      ) {
        address(
          addressLine1 = "Hospital House, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
        phoneNumber(
          phoneNumber = "0114 555 8989",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      if (::hospital.isInitialized) {
        hospitalRepository.findByIdOrNull(hospital.hospitalId)?.let { hospitalRepository.deleteById(hospital.hospitalId) }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/hospitals/id/SHFHOS")
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
      fun `will delete the hospital, along with its addresses and phone numbers`() {
        val addressId = hospital.addresses[0].id
        val phoneNumberId = hospital.phoneNumbers[0].id

        webTestClient.delete()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(hospitalRepository.findByIdOrNull("SHFHOS")).isNull()
          assertThat(agencyAddressRepository.findByIdOrNull(addressId)).isNull()
          assertThat(phoneNumberRepository.findByIdOrNull(phoneNumberId)).isNull()
        }

        verify(telemetryClient).trackEvent(
          eq("hospital-deleted"),
          check {
            assertThat(it["hospitalId"]).isEqualTo("SHFHOS")
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Delete hospital address")
  @Nested
  inner class DeleteHospitalAddress {
    lateinit var hospital: Hospital
    var addressId: Long = -1

    @BeforeEach
    fun setUp() {
      hospital = dsl.hospital(
        hospitalId = "SHFHOS",
        name = "Sheffield Secure Hospital",
        description = "Sheffield Central Secure Hospital",
        active = true,
        highSecurity = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "HS",
      ) {
        address(
          addressLine1 = "Hospital House, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
      }
      addressId = hospital.addresses[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::hospital.isInitialized) {
        hospitalRepository.findByIdOrNull(hospital.hospitalId)?.let { hospitalRepository.deleteById(hospital.hospitalId) }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/hospitals/id/SHFHOS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/hospitals/id/SHFHOS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if hospital not found`() {
        webTestClient.delete()
          .uri("/hospitals/id/ZZZZ/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if address not found`() {
        webTestClient.delete()
          .uri("/hospitals/id/SHFHOS/address/{addressId}", 999999)
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
          .uri("/hospitals/id/SHFHOS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(agencyAddressRepository.findByIdOrNull(addressId)).isNull()
          assertThat(hospitalRepository.findByIdOrNull("SHFHOS")?.addresses).isEmpty()
        }

        verify(telemetryClient).trackEvent(
          eq("hospital-address-deleted"),
          check {
            assertThat(it["hospitalId"]).isEqualTo("SHFHOS")
            assertThat(it["addressId"]).isEqualTo(addressId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Delete hospital phone number")
  @Nested
  inner class DeleteHospitalPhoneNumber {
    lateinit var hospital: Hospital
    var phoneNumberId: Long = -1

    @BeforeEach
    fun setUp() {
      hospital = dsl.hospital(
        hospitalId = "SHFHOS",
        name = "Sheffield Secure Hospital",
        description = "Sheffield Central Secure Hospital",
        active = true,
        highSecurity = true,
        inactiveDate = null,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "HS",
      ) {
        phoneNumber(
          phoneNumber = "0114 555 8989",
        )
      }
      phoneNumberId = hospital.phoneNumbers[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::hospital.isInitialized) {
        hospitalRepository.findByIdOrNull(hospital.hospitalId)?.let { hospitalRepository.deleteById(hospital.hospitalId) }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.delete()
          .uri("/hospitals/id/SHFHOS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.delete()
          .uri("/hospitals/id/SHFHOS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if hospital not found`() {
        webTestClient.delete()
          .uri("/hospitals/id/ZZZZ/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if phone number not found`() {
        webTestClient.delete()
          .uri("/hospitals/id/SHFHOS/phone-number/{phoneNumberId}", 999999)
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
          .uri("/hospitals/id/SHFHOS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNoContent

        transactionHelper.runInTransaction {
          assertThat(phoneNumberRepository.findByIdOrNull(phoneNumberId)).isNull()
          assertThat(hospitalRepository.findByIdOrNull("SHFHOS")?.phoneNumbers).isEmpty()
        }

        verify(telemetryClient).trackEvent(
          eq("hospital-phone-number-deleted"),
          check {
            assertThat(it["hospitalId"]).isEqualTo("SHFHOS")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneNumberId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Update hospital")
  @Nested
  inner class UpdateHospital {
    lateinit var hospital: Hospital

    val updateHospitalRequest = UpdateHospitalDto(
      hospitalName = "Sheffield Secure Hospital Updated",
      description = "Sheffield Central Secure Hospital Updated",
      active = true,
      inactiveDate = null,
      cjitCode = "123456789",
      areaCode = "52",
      regionCode = "YOHUM",
      geographicalAreaCode = "WYORKS",
      localAuthorityCode = "00CG",
      payrollRegionCode = "NEY",
      highSecurity = false,
    )

    @BeforeEach
    fun setUp() {
      hospital = dsl.hospital(
        hospitalId = "SHFHOS",
        name = "Sheffield Secure Hospital",
        description = "Sheffield Central Secure Hospital",
        active = false,
        inactiveDate = LocalDate.parse("2020-01-02"),
        highSecurity = true,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "HS",
      ) {
        address(
          addressLine1 = "Hospital House, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
        phoneNumber(
          phoneNumber = "0114 555 8989",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      if (::hospital.isInitialized) {
        hospitalRepository.findByIdOrNull(hospital.hospitalId)?.let { hospitalRepository.deleteById(hospital.hospitalId) }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateHospitalRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateHospitalRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateHospitalRequest)
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if not found`() {
        webTestClient.put()
          .uri("/hospitals/id/ZZZZ")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateHospitalRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateHospitalRequest.copy(areaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for hospital SHFHOS")
      }

      @Test
      fun `region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateHospitalRequest.copy(regionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for hospital SHFHOS")
      }

      @Test
      fun `geographical area code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateHospitalRequest.copy(geographicalAreaCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for hospital SHFHOS")
      }

      @Test
      fun `local authority code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateHospitalRequest.copy(localAuthorityCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for hospital SHFHOS")
      }

      @Test
      fun `payroll region code is not valid`() {
        val errorResponse: ErrorResponse = webTestClient.put()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateHospitalRequest.copy(payrollRegionCode = "ZZZ"))
          .exchange()
          .expectStatus().isBadRequest.expectBodyResponse()

        assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for hospital SHFHOS")
      }

      @Test
      fun `hospital name is blank`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateHospitalRequest.copy(hospitalName = ""))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the core hospital data`() {
        val hospitalDto: HospitalDto = webTestClient.put()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateHospitalRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(hospitalDto.hospitalId).isEqualTo("SHFHOS")
        assertThat(hospitalDto.hospitalName).isEqualTo("Sheffield Secure Hospital Updated")
        assertThat(hospitalDto.description).isEqualTo("Sheffield Central Secure Hospital Updated")
        assertThat(hospitalDto.active).isFalse
        assertThat(hospitalDto.inactiveDate).isEqualTo("2026-01-01")
        assertThat(hospitalDto.cjitCode).isEqualTo("123456789")
        assertThat(hospitalDto.highSecurity).isFalse
        assertThat(hospitalDto.area?.description).isEqualTo("South Yorkshire")
        assertThat(hospitalDto.region?.description).isEqualTo("Yorkshire & Humberside")
        assertThat(hospitalDto.geographicalArea?.description).isEqualTo("West Yorkshire")
        assertThat(hospitalDto.localAuthority?.description).isEqualTo("Sheffield City Council")
        assertThat(hospitalDto.payrollRegion?.code).isEqualTo("NEY")

        verify(telemetryClient).trackEvent(
          eq("hospital-updated"),
          check {
            assertThat(it["hospitalId"]).isEqualTo("SHFHOS")
          },
          isNull(),
        )
      }

      @Test
      fun `will not affect addresses or phone numbers`() {
        val hospitalDto: HospitalDto = webTestClient.put()
          .uri("/hospitals/id/SHFHOS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateHospitalRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(hospitalDto.addresses).hasSize(1)
        assertThat(hospitalDto.addresses[0].addressLine1).isEqualTo("Hospital House, 31 High Street")
        assertThat(hospitalDto.phoneNumbers).hasSize(1)
        assertThat(hospitalDto.phoneNumbers[0].number).isEqualTo("0114 555 8989")

        verify(telemetryClient).trackEvent(
          eq("hospital-updated"),
          check {
            assertThat(it["hospitalId"]).isEqualTo("SHFHOS")
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Update hospital address")
  @Nested
  inner class UpdateHospitalAddress {
    lateinit var hospital: Hospital
    var addressId: Long = -1

    val updateAddressRequest = UpdateAddressDto(
      addressLine1 = "Updated Hospital House, 31 High Street",
      addressLine2 = "Updated City Centre",
      town = "Updated Sheffield",
      county = "Updated South Yorkshire",
      postcode = "S1 4HH",
      country = "Wales",
    )

    @BeforeEach
    fun setUp() {
      hospital = dsl.hospital(
        hospitalId = "SHFHOS",
        name = "Sheffield Secure Hospital",
        description = "Sheffield Central Secure Hospital",
        active = true,
        highSecurity = true,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "HS",
      ) {
        address(
          addressLine1 = "Hospital House, 31 High Street",
          town = "Sheffield",
          postcode = "S1 3GG",
          country = "England",
        )
      }
      addressId = hospital.addresses[0].id
    }

    @AfterEach
    fun tearDown() {
      if (::hospital.isInitialized) {
        hospitalRepository.findByIdOrNull(hospital.hospitalId)?.let { hospitalRepository.deleteById(hospital.hospitalId) }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS/address/{addressId}", addressId)
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
      fun `404 if hospital not found`() {
        webTestClient.put()
          .uri("/hospitals/id/ZZZZ/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if address not found`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS/address/{addressId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if town is missing`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(mapOf("postcode" to "S1 3GG", "country" to "England"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if postcode is too long`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS/address/{addressId}", addressId)
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
          .uri("/hospitals/id/SHFHOS/address/{addressId}", addressId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updateAddressRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(addressDto.id).isEqualTo(addressId)
        assertThat(addressDto.addressLine1).isEqualTo("Updated Hospital House, 31 High Street")
        assertThat(addressDto.addressLine2).isEqualTo("Updated City Centre")
        assertThat(addressDto.town).isEqualTo("Updated Sheffield")
        assertThat(addressDto.county).isEqualTo("Updated South Yorkshire")
        assertThat(addressDto.postcode).isEqualTo("S1 4HH")
        assertThat(addressDto.country).isEqualTo("Wales")

        verify(telemetryClient).trackEvent(
          eq("hospital-address-updated"),
          check {
            assertThat(it["hospitalId"]).isEqualTo("SHFHOS")
            assertThat(it["addressId"]).isEqualTo(addressId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("Update hospital phone number")
  @Nested
  inner class UpdateHospitalPhoneNumber {
    lateinit var hospital: Hospital
    var phoneNumberId: Long = -1

    val updatePhoneNumberRequest = UpdatePhoneNumberDto(number = "0114 555 1234")

    @BeforeEach
    fun setUp() {
      hospital = dsl.hospital(
        hospitalId = "SHFHOS",
        name = "Sheffield Secure Hospital",
        description = "Sheffield Central Secure Hospital",
        active = true,
        highSecurity = true,
        cjitCode = "C00SH00",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "HS",
      ) {
        phoneNumber(
          phoneNumber = "0114 555 8989",
        )
        phoneNumber(
          phoneNumber = "0114 555 4321",
        )
      }
      phoneNumberId = hospital.phoneNumbers.first { it.value == "0114 555 8989" }.id
    }

    @AfterEach
    fun tearDown() {
      if (::hospital.isInitialized) {
        hospitalRepository.findByIdOrNull(hospital.hospitalId)?.let { hospitalRepository.deleteById(hospital.hospitalId) }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS/phone-number/{phoneNumberId}", phoneNumberId)
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
      fun `404 if hospital not found`() {
        webTestClient.put()
          .uri("/hospitals/id/ZZZZ/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `404 if phone number not found`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS/phone-number/{phoneNumberId}", 999999)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if phone number is in an incorrect format`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = "not-a-number"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `400 if phone number is blank`() {
        webTestClient.put()
          .uri("/hospitals/id/SHFHOS/phone-number/{phoneNumberId}", phoneNumberId)
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
          .uri("/hospitals/id/SHFHOS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest)
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(phoneDto.id).isEqualTo(phoneNumberId)
        assertThat(phoneDto.number).isEqualTo("0114 555 1234")

        verify(telemetryClient).trackEvent(
          eq("hospital-phone-number-updated"),
          check {
            assertThat(it["hospitalId"]).isEqualTo("SHFHOS")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneNumberId.toString())
          },
          isNull(),
        )
      }

      @Test
      fun `will allow updating a phone number to its own current value`() {
        val phoneDto: AgencyPhoneDto = webTestClient.put()
          .uri("/hospitals/id/SHFHOS/phone-number/{phoneNumberId}", phoneNumberId)
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(updatePhoneNumberRequest.copy(number = "0114 555 8989"))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(phoneDto.number).isEqualTo("0114 555 8989")

        verify(telemetryClient).trackEvent(
          eq("hospital-phone-number-updated"),
          check {
            assertThat(it["hospitalId"]).isEqualTo("SHFHOS")
            assertThat(it["phoneNumberId"]).isEqualTo(phoneNumberId.toString())
          },
          isNull(),
        )
      }
    }
  }
}
