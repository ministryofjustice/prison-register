package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.Hospital
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class LegacySyncHospitalResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var hospitalRepository: HospitalRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @Autowired
  lateinit var dsl: Root

  @MockitoBean
  lateinit var telemetry: TelemetryClient

  @AfterEach
  fun tearDown() {
    hospitalRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /legacy/sync/agency/id/{agencyId}")
  inner class CreateOrUpdateAgency {

    @Nested
    inner class WhenHospital {
      val hospitalRequest = LegacyAgencyDto(
        agencyType = LegacyAgencyType.HOSPITAL,
        name = "Broadmoor Hospital",
        description = "Broadmoor High Security Hospital",
        active = true,
        inactiveDate = null,
        cjitCode = "123456789",
        contact = null,
        accessibleAccess = null,
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "HS",
        localAuthorityCode = "00CG",
        courtTypeCode = null,
        addresses = listOf(
          LegacyAgencyAddressDto(
            addressLine1 = "Crowthorne",
            addressLine2 = null,
            town = "Berkshire",
            county = "Berkshire",
            postcode = "RG45 7EG",
            country = "England",
          ),
        ),
        emailAddresses = listOf(),
        phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "01344 773111")),
      )

      @Nested
      inner class Create {

        @Nested
        inner class Validation {
          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(hospitalRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency BRDMR")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(hospitalRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency BRDMR")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(hospitalRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency BRDMR")
          }

          @Test
          fun `payroll region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(hospitalRequest.copy(payrollRegionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for agency BRDMR")
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will create a hospital with highSecurity false when type is HOSPITAL`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                hospitalRequest.copy(
                  agencyType = LegacyAgencyType.HOSPITAL,
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isFalse

            transactionHelper.runInTransaction {
              val hospital = hospitalRepository.findByIdOrNull("BRDMR")!!

              with(hospital) {
                assertThat(name).isEqualTo("Broadmoor Hospital")
                assertThat(description).isEqualTo("Broadmoor High Security Hospital")
                assertThat(active).isTrue
                assertThat(highSecurity).isFalse
                assertThat(inactiveDate).isNull()
                assertThat(cjitCode).isEqualTo("123456789")
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(geographicalArea?.description).isEqualTo("West Yorkshire")
                assertThat(payrollRegion?.description).isEqualTo("High Security")
                assertThat(addresses).isEmpty()
                assertThat(phoneNumbers).isEmpty()
              }
            }
          }

          @Test
          fun `will create a hospital with highSecurity true when type is SECURE_HOSPITAL`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                hospitalRequest.copy(
                  agencyType = LegacyAgencyType.SECURE_HOSPITAL,
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isFalse

            transactionHelper.runInTransaction {
              val hospital = hospitalRepository.findByIdOrNull("BRDMR")!!

              with(hospital) {
                assertThat(highSecurity).isTrue
              }
            }
          }

          @Test
          fun `will create a hospital address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                hospitalRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "Crowthorne",
                      addressLine2 = null,
                      town = "Berkshire",
                      county = "Berkshire",
                      postcode = "RG45 7EG",
                      country = "England",
                    ),
                  ),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(hospitalRepository.findByIdOrNull("BRDMR")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses[0]) {
                  assertThat(addressLine1).isEqualTo("Crowthorne")
                  assertThat(addressLine2).isNull()
                  assertThat(town).isEqualTo("Berkshire")
                  assertThat(county).isEqualTo("Berkshire")
                  assertThat(postcode).isEqualTo("RG45 7EG")
                  assertThat(country).isEqualTo("England")
                }
              }
            }
          }

          @Test
          fun `will create hospital phone numbers`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                hospitalRequest.copy(
                  addresses = emptyList(),
                  phoneNumbers = listOf(
                    LegacyAgencyPhoneDto(number = "01344 773111"),
                    LegacyAgencyPhoneDto(number = "01344 773222"),
                  ),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(hospitalRepository.findByIdOrNull("BRDMR")!!) {
                assertThat(phoneNumbers).hasSize(2)
                with(phoneNumbers[0]) {
                  assertThat(value).isEqualTo("01344 773111")
                }
                with(phoneNumbers[1]) {
                  assertThat(value).isEqualTo("01344 773222")
                }
              }
            }
          }
        }
      }

      @Nested
      inner class Update {
        val updateRequest = LegacyAgencyDto(
          agencyType = LegacyAgencyType.HOSPITAL,
          name = "Broadmoor Hospital",
          description = "Broadmoor High Security Hospital",
          active = true,
          inactiveDate = null,
          contact = null,
          cjitCode = "123456789",
          accessibleAccess = null,
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = "HS",
          localAuthorityCode = "00CG",
          courtTypeCode = null,
          addresses = listOf(
            LegacyAgencyAddressDto(
              addressLine1 = "Crowthorne",
              addressLine2 = null,
              town = "Berkshire",
              county = "Berkshire",
              postcode = "RG45 7EG",
              country = "England",
            ),
          ),
          emailAddresses = listOf(),
          phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "01344 773111")),
        )

        lateinit var hospital: Hospital

        @BeforeEach
        fun setUp() {
          hospital = dsl.hospital(
            hospitalId = "BRDMR",
            name = "Broadmoor Hospital",
            description = "Broadmoor High Security Hospital",
            active = true,
            highSecurity = false,
            inactiveDate = null,
            cjitCode = "123456789",
            areaCode = "52",
            regionCode = "YOHUM",
            geographicalAreaCode = "WYORKS",
            payrollRegionCode = "HS",
          ) {
            address(
              addressLine1 = "Crowthorne",
              addressLine2 = null,
              town = "Berkshire",
              county = "Berkshire",
              postcode = "RG45 7EG",
              country = "England",
            )
            phoneNumber(phoneNumber = "01344 773111")
          }
        }

        @Nested
        inner class Validation {
          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency BRDMR")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency BRDMR")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(hospitalRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency BRDMR")
          }

          @Test
          fun `payroll region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(hospitalRequest.copy(payrollRegionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for agency BRDMR")
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will update core hospital data`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01")))
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isTrue

            transactionHelper.runInTransaction {
              val updated = hospitalRepository.findByIdOrNull("BRDMR")!!

              with(updated) {
                assertThat(name).isEqualTo("Broadmoor Hospital")
                assertThat(description).isEqualTo("Broadmoor High Security Hospital")
                assertThat(active).isFalse
                assertThat(inactiveDate).isEqualTo("2026-01-01")
                assertThat(highSecurity).isFalse
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
              }
            }
          }

          @Test
          fun `will update highSecurity to true when type changes to SECURE_HOSPITAL`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(agencyType = LegacyAgencyType.SECURE_HOSPITAL))
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              assertThat(hospitalRepository.findByIdOrNull("BRDMR")!!.highSecurity).isTrue
            }
          }

          @Test
          fun `will update existing hospital address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "Crowthorne",
                      addressLine2 = "Main Entrance",
                      town = "Berkshire",
                      county = "Berkshire",
                      postcode = "RG45 7EG",
                      country = "England",
                    ),
                  ),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(hospitalRepository.findByIdOrNull("BRDMR")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses[0]) {
                  assertThat(addressLine1).isEqualTo("Crowthorne")
                  assertThat(addressLine2).isEqualTo("Main Entrance")
                }
              }
            }
          }

          @Test
          fun `will remove existing hospital address`() {
            transactionHelper.runInTransaction {
              assertThat(hospitalRepository.findByIdOrNull("BRDMR")!!.addresses).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(addresses = emptyList(), phoneNumbers = emptyList()))
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              assertThat(hospitalRepository.findByIdOrNull("BRDMR")!!.addresses).isEmpty()
            }
          }

          @Test
          fun `will update hospital phone numbers`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = emptyList(),
                  phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "01344 999000")),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(hospitalRepository.findByIdOrNull("BRDMR")!!) {
                assertThat(phoneNumbers).hasSize(1)
                assertThat(phoneNumbers[0].value).isEqualTo("01344 999000")
              }
            }
          }

          @Test
          fun `will remove hospital phone numbers`() {
            transactionHelper.runInTransaction {
              assertThat(hospitalRepository.findByIdOrNull("BRDMR")!!.phoneNumbers).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "BRDMR")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(addresses = emptyList(), phoneNumbers = emptyList()))
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              assertThat(hospitalRepository.findByIdOrNull("BRDMR")!!.phoneNumbers).isEmpty()
            }
          }
        }
      }
    }
  }
}
