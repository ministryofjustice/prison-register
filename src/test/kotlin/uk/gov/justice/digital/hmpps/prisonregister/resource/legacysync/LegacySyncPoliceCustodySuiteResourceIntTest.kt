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
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuite
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class LegacySyncPoliceCustodySuiteResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var policeCustodySuiteRepository: PoliceCustodySuiteRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @Autowired
  lateinit var dsl: Root

  @MockitoBean
  lateinit var telemetry: TelemetryClient

  @AfterEach
  fun tearDown() {
    policeCustodySuiteRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /legacy/sync/agency/id/{agencyId}")
  inner class CreateOrUpdateAgency {

    @Nested
    inner class WhenPoliceCustodySuite {
      val policeCustodySuiteRequest = LegacyAgencyDto(
        agencyType = LegacyAgencyType.POLICE_CUSTODY_SUITE,
        name = "Sheffield Police Station",
        description = "Sheffield Central Police Station",
        active = true,
        inactiveDate = null,
        cjitCode = "123456789",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = null,
        localAuthorityCode = "00CG",
        courtTypeCode = null,
        accessibleAccess = null,
        contact = null,
        addresses = listOf(
          LegacyAgencyAddressDto(
            addressLine1 = "101 Snig Hill",
            addressLine2 = null,
            town = "Sheffield",
            county = "South Yorkshire",
            postcode = "S3 8LY",
            country = "England",
          ),
        ),
        emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.police@example.com")),
        phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 220 2020")),
      )

      @Nested
      inner class Create {

        @Nested
        inner class Validation {
          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(policeCustodySuiteRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency SHEFPS")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(policeCustodySuiteRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency SHEFPS")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(policeCustodySuiteRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SHEFPS")
          }

          @Test
          fun `local authority code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(policeCustodySuiteRequest.copy(localAuthorityCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for agency SHEFPS")
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will create the core police custody suite data`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                policeCustodySuiteRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isFalse

            transactionHelper.runInTransaction {
              val policeCustodySuite = policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!

              with(policeCustodySuite) {
                assertThat(name).isEqualTo("Sheffield Police Station")
                assertThat(description).isEqualTo("Sheffield Central Police Station")
                assertThat(active).isTrue
                assertThat(inactiveDate).isNull()
                assertThat(cjitCode).isEqualTo("123456789")
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(geographicalArea?.description).isEqualTo("West Yorkshire")
                assertThat(localAuthority?.description).isEqualTo("Sheffield City Council")
                assertThat(addresses).isEmpty()
                assertThat(phoneNumbers).isEmpty()
                assertThat(emailAddresses).isEmpty()
              }
            }
          }

          @Test
          fun `will create an address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                policeCustodySuiteRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "101 Snig Hill",
                      addressLine2 = null,
                      town = "Sheffield",
                      county = "South Yorkshire",
                      postcode = "S3 8LY",
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
              with(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses[0]) {
                  assertThat(addressLine1).isEqualTo("101 Snig Hill")
                  assertThat(addressLine2).isNull()
                  assertThat(town).isEqualTo("Sheffield")
                  assertThat(county).isEqualTo("South Yorkshire")
                  assertThat(postcode).isEqualTo("S3 8LY")
                  assertThat(country).isEqualTo("England")
                }
              }
            }
          }

          @Test
          fun `will create an email address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                policeCustodySuiteRequest.copy(
                  emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.police@example.com")),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!) {
                assertThat(emailAddresses).hasSize(1)
                assertThat(emailAddresses[0].value).isEqualTo("sheffield.police@example.com")
              }
            }
          }

          @Test
          fun `will create phone numbers`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                policeCustodySuiteRequest.copy(
                  emailAddresses = emptyList(),
                  addresses = emptyList(),
                  phoneNumbers = listOf(
                    LegacyAgencyPhoneDto(number = "0114 220 2020"),
                    LegacyAgencyPhoneDto(number = "0114 220 3030"),
                  ),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!) {
                assertThat(phoneNumbers).hasSize(2)
                assertThat(phoneNumbers[0].value).isEqualTo("0114 220 2020")
                assertThat(phoneNumbers[1].value).isEqualTo("0114 220 3030")
              }
            }
          }
        }
      }

      @Nested
      inner class Update {
        val updateRequest = LegacyAgencyDto(
          agencyType = LegacyAgencyType.POLICE_CUSTODY_SUITE,
          name = "Sheffield Police Station",
          description = "Sheffield Central Police Station",
          active = true,
          inactiveDate = null,
          cjitCode = "123456789",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = null,
          localAuthorityCode = "00CG",
          courtTypeCode = null,
          accessibleAccess = null,
          contact = null,
          addresses = listOf(
            LegacyAgencyAddressDto(
              addressLine1 = "101 Snig Hill",
              addressLine2 = null,
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S3 8LY",
              country = "England",
            ),
          ),
          emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.police@example.com")),
          phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 220 2020")),
        )

        lateinit var policeCustodySuite: PoliceCustodySuite

        @BeforeEach
        fun setUp() {
          policeCustodySuite = dsl.policeCustodySuite(
            policeCustodySuiteId = "SHEFPS",
            name = "Sheffield Police Station",
            description = "Sheffield Central Police Station",
            active = true,
            inactiveDate = null,
            cjitCode = "123456789",
            areaCode = "52",
            regionCode = "YOHUM",
            geographicalAreaCode = "WYORKS",
            localAuthorityCode = "00CF",
          ) {
            address(
              addressLine1 = "101 Snig Hill",
              addressLine2 = null,
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S3 8LY",
              country = "England",
            )
            email(emailAddress = "sheffield.police@example.com")
            phoneNumber(phoneNumber = "0114 220 2020")
          }
        }

        @Nested
        inner class Validation {
          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency SHEFPS")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency SHEFPS")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SHEFPS")
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will update the core police custody suite data`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01")))
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isTrue

            transactionHelper.runInTransaction {
              val updated = policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!

              with(updated) {
                assertThat(name).isEqualTo("Sheffield Police Station")
                assertThat(description).isEqualTo("Sheffield Central Police Station")
                assertThat(active).isFalse
                assertThat(inactiveDate).isEqualTo(LocalDate.parse("2026-01-01"))
                assertThat(cjitCode).isEqualTo("123456789")
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(geographicalArea?.description).isEqualTo("West Yorkshire")
                assertThat(localAuthority?.description).isEqualTo("Sheffield City Council")
              }
            }
          }

          @Test
          fun `will update existing address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "101 Snig Hill",
                      addressLine2 = "Rear Entrance",
                      town = "Sheffield",
                      county = "South Yorkshire",
                      postcode = "S3 8LY",
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
              with(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses[0]) {
                  assertThat(addressLine1).isEqualTo("101 Snig Hill")
                  assertThat(addressLine2).isEqualTo("Rear Entrance")
                }
              }
            }
          }

          @Test
          fun `will remove existing address`() {
            transactionHelper.runInTransaction {
              assertThat(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!.addresses).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
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
              assertThat(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!.addresses).isEmpty()
            }
          }

          @Test
          fun `will update email addresses`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  emailAddresses = listOf(LegacyAgencyEmailDto(address = "new.sheffield.police@example.com")),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!) {
                assertThat(emailAddresses).hasSize(1)
                assertThat(emailAddresses[0].value).isEqualTo("new.sheffield.police@example.com")
              }
            }
          }

          @Test
          fun `will remove email addresses`() {
            transactionHelper.runInTransaction {
              assertThat(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!.emailAddresses).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
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
              assertThat(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!.emailAddresses).isEmpty()
            }
          }

          @Test
          fun `will update phone numbers`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 220 9999")),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!) {
                assertThat(phoneNumbers).hasSize(1)
                assertThat(phoneNumbers[0].value).isEqualTo("0114 220 9999")
              }
            }
          }

          @Test
          fun `will remove phone numbers`() {
            transactionHelper.runInTransaction {
              assertThat(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!.phoneNumbers).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFPS")
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
              assertThat(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!.phoneNumbers).isEmpty()
            }
          }
        }
      }
    }
  }
}
