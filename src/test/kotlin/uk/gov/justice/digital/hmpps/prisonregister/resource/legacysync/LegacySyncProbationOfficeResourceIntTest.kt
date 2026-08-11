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
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOffice
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class LegacySyncProbationOfficeResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var probationOfficeRepository: ProbationOfficeRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @Autowired
  lateinit var dsl: Root

  @MockitoBean
  lateinit var telemetry: TelemetryClient

  @AfterEach
  fun tearDown() {
    probationOfficeRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /legacy/sync/agency/id/{agencyId}")
  inner class CreateOrUpdateAgency {

    @Nested
    inner class WhenProbationOffice {
      val probationOfficeRequest = LegacyAgencyDto(
        agencyType = LegacyAgencyType.PROBATION_OFFICE,
        name = "Sheffield Probation Office",
        description = "Sheffield City Centre Probation Office",
        active = true,
        inactiveDate = null,
        contact = null,
        cjitCode = "123456789",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = null,
        courtTypeCode = null,
        accessibleAccess = LegacyAccessibleAccess.WHEELCHAIR_ACCESS,
        addresses = listOf(
          LegacyAgencyAddressDto(
            addressLine1 = "Probation House, 31 High Street",
            addressLine2 = "City Centre",
            town = "Sheffield",
            county = "South Yorkshire",
            postcode = "S1 3GG",
            country = "England",
          ),
        ),
        emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.probation@justice.gov.uk")),
        phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 7777")),
      )

      @Nested
      inner class Create {

        @Nested
        inner class Validation {
          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(probationOfficeRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency SHEFPB")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(probationOfficeRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency SHEFPB")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(probationOfficeRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SHEFPB")
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will create the core probation office data`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                probationOfficeRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isFalse

            transactionHelper.runInTransaction {
              val probationOffice = probationOfficeRepository.findByIdOrNull("SHEFPB")!!

              with(probationOffice) {
                assertThat(name).isEqualTo("Sheffield Probation Office")
                assertThat(description).isEqualTo("Sheffield City Centre Probation Office")
                assertThat(active).isTrue
                assertThat(inactiveDate).isNull()
                assertThat(cjitCode).isEqualTo("123456789")
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(geographicalArea?.description).isEqualTo("West Yorkshire")
                assertThat(accessibleAccess).isEqualTo(AccessibleAccess.WHEELCHAIR_ACCESS)
                assertThat(addresses).isEmpty()
                assertThat(phoneNumbers).isEmpty()
                assertThat(emailAddresses).isEmpty()
              }
            }
          }

          @Test
          fun `will create an address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                probationOfficeRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "Probation House, 31 High Street",
                      addressLine2 = "City Centre",
                      town = "Sheffield",
                      county = "South Yorkshire",
                      postcode = "S1 3GG",
                      country = "England",
                    ),
                  ),
                  emailAddresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(probationOfficeRepository.findByIdOrNull("SHEFPB")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses[0]) {
                  assertThat(addressLine1).isEqualTo("Probation House, 31 High Street")
                  assertThat(addressLine2).isEqualTo("City Centre")
                  assertThat(town).isEqualTo("Sheffield")
                  assertThat(county).isEqualTo("South Yorkshire")
                  assertThat(postcode).isEqualTo("S1 3GG")
                  assertThat(country).isEqualTo("England")
                }
              }
            }
          }

          @Test
          fun `will create an email address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                probationOfficeRequest.copy(
                  emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.probation@justice.gov.uk")),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(probationOfficeRepository.findByIdOrNull("SHEFPB")!!) {
                assertThat(emailAddresses).hasSize(1)
                assertThat(emailAddresses[0].value).isEqualTo("sheffield.probation@justice.gov.uk")
              }
            }
          }

          @Test
          fun `will create phone numbers`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                probationOfficeRequest.copy(
                  emailAddresses = emptyList(),
                  addresses = emptyList(),
                  phoneNumbers = listOf(
                    LegacyAgencyPhoneDto(number = "0114 555 7777"),
                    LegacyAgencyPhoneDto(number = "0114 999 7777"),
                  ),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(probationOfficeRepository.findByIdOrNull("SHEFPB")!!) {
                assertThat(phoneNumbers).hasSize(2)
                assertThat(phoneNumbers[0].value).isEqualTo("0114 555 7777")
                assertThat(phoneNumbers[1].value).isEqualTo("0114 999 7777")
              }
            }
          }
        }
      }

      @Nested
      inner class Update {
        val updateRequest = LegacyAgencyDto(
          agencyType = LegacyAgencyType.PROBATION_OFFICE,
          name = "Sheffield Probation Office",
          description = "Sheffield City Centre Probation Office",
          active = true,
          inactiveDate = null,
          cjitCode = "123456789",
          contact = null,
          accessibleAccess = null,
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = null,
          courtTypeCode = null,
          addresses = listOf(
            LegacyAgencyAddressDto(
              addressLine1 = "Probation House, 31 High Street",
              addressLine2 = "City Centre",
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S1 3GG",
              country = "England",
            ),
          ),
          emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.probation@justice.gov.uk")),
          phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 7777")),
        )

        lateinit var probationOffice: ProbationOffice

        @BeforeEach
        fun setUp() {
          probationOffice = dsl.probationOffice(
            probationOfficeId = "SHEFPB",
            name = "Sheffield Probation Office",
            description = "Sheffield City Centre Probation Office",
            active = true,
            inactiveDate = null,
            cjitCode = "123456789",
            areaCode = "52",
            regionCode = "YOHUM",
            accessibleAccess = AccessibleAccess.NONE,
            geographicalAreaCode = "WYORKS",
          ) {
            address(
              addressLine1 = "Probation House, 31 High Street",
              addressLine2 = "City Centre",
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S1 3GG",
              country = "England",
            )
            email(emailAddress = "sheffield.probation@justice.gov.uk")
            phoneNumber(phoneNumber = "0114 555 7777")
          }
        }

        @Nested
        inner class Validation {
          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency SHEFPB")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency SHEFPB")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SHEFPB")
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will update the core probation office data`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  active = false,
                  inactiveDate = LocalDate.parse("2026-01-01"),
                  accessibleAccess = LegacyAccessibleAccess.BY_ARRANGEMENT_ONLY,
                ),
              )
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isTrue

            transactionHelper.runInTransaction {
              val updated = probationOfficeRepository.findByIdOrNull("SHEFPB")!!

              with(updated) {
                assertThat(name).isEqualTo("Sheffield Probation Office")
                assertThat(description).isEqualTo("Sheffield City Centre Probation Office")
                assertThat(active).isFalse
                assertThat(inactiveDate).isEqualTo(LocalDate.parse("2026-01-01"))
                assertThat(cjitCode).isEqualTo("123456789")
                assertThat(accessibleAccess).isEqualTo(AccessibleAccess.BY_ARRANGEMENT_ONLY)
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(geographicalArea?.description).isEqualTo("West Yorkshire")
              }
            }
          }

          @Test
          fun `will update existing address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "Probation House, 31 High Street",
                      addressLine2 = "Floor 2",
                      town = "Sheffield",
                      county = "South Yorkshire",
                      postcode = "S1 3GG",
                      country = "England",
                    ),
                  ),
                  emailAddresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(probationOfficeRepository.findByIdOrNull("SHEFPB")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses[0]) {
                  assertThat(addressLine1).isEqualTo("Probation House, 31 High Street")
                  assertThat(addressLine2).isEqualTo("Floor 2")
                }
              }
            }
          }

          @Test
          fun `will remove existing address`() {
            transactionHelper.runInTransaction {
              assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")!!.addresses).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")!!.addresses).isEmpty()
            }
          }

          @Test
          fun `will update email addresses`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  emailAddresses = listOf(LegacyAgencyEmailDto(address = "new.sheffield.probation@justice.gov.uk")),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(probationOfficeRepository.findByIdOrNull("SHEFPB")!!) {
                assertThat(emailAddresses).hasSize(1)
                assertThat(emailAddresses[0].value).isEqualTo("new.sheffield.probation@justice.gov.uk")
              }
            }
          }

          @Test
          fun `will remove email addresses`() {
            transactionHelper.runInTransaction {
              assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")!!.emailAddresses).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")!!.emailAddresses).isEmpty()
            }
          }

          @Test
          fun `will update phone numbers`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 999 7777")),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(probationOfficeRepository.findByIdOrNull("SHEFPB")!!) {
                assertThat(phoneNumbers).hasSize(1)
                assertThat(phoneNumbers[0].value).isEqualTo("0114 999 7777")
              }
            }
          }

          @Test
          fun `will remove phone numbers`() {
            transactionHelper.runInTransaction {
              assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")!!.phoneNumbers).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPB")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")!!.phoneNumbers).isEmpty()
            }
          }
        }
      }
    }
  }
}
