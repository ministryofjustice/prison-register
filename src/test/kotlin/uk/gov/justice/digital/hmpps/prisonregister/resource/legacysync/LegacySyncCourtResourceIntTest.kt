package uk.gov.justice.digital.hmpps.prisonregister.resource.legacysync

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
import uk.gov.justice.digital.hmpps.prisonregister.model.Court
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class LegacySyncCourtResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var courtRepository: CourtRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @Autowired
  lateinit var dsl: Root

  @MockitoBean
  lateinit var telemetry: TelemetryClient

  @AfterEach
  fun tearDown() {
    courtRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /legacy/sync/agency/id/{agencyId}")
  inner class CreateOrUpdateAgency {

    @Nested
    inner class WhenCourt {

      @Nested
      inner class Create {
        val courtRequest = LegacyAgencyDto(
          agencyType = LegacyAgencyType.COURT,
          name = "Sheffield MC",
          description = "Sheffield Magistrates' Court",
          active = true,
          inactiveDate = null,
          contact = null,
          cjitCode = "123456789",
          accessibleAccess = LegacyAccessibleAccess.WHEELCHAIR_ACCESS,
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = "NEY",
          localAuthorityCode = "00CG",
          courtTypeCode = "MC",
          addresses = listOf(
            LegacyAgencyAddressDto(
              addressLine1 = "Castle Street",
              addressLine2 = null,
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S3 8LU",
              country = "England",
            ),
          ),
          emailAddresses = listOf(LegacyAgencyEmailDto(address = "test.sheffield.mc@justice.gov.uk")),
          phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 5555")),
        )

        @Nested
        inner class Validation {
          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency SHEFMC")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency SHEFMC")
          }

          @Test
          fun `court type code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(courtTypeCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ court type not found for agency SHEFMC")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SHEFMC")
          }

          @Test
          fun `local authority code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(localAuthorityCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for agency SHEFMC")
          }

          @Test
          fun `payroll region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(payrollRegionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for agency SHEFMC")
          }

          @Test
          fun `court type code missing is mapped to unknown`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(courtTypeCode = null))
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              val court = courtRepository.findByIdOrNull("SHEFMC")!!

              with(court) {
                assertThat(courtType.description).isEqualTo("Unknown")
              }
            }
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will create the core court data`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isFalse

            transactionHelper.runInTransaction {
              val court = courtRepository.findByIdOrNull("SHEFMC")!!

              with(court) {
                assertThat(name).isEqualTo("Sheffield MC")
                assertThat(description).isEqualTo("Sheffield Magistrates' Court")
                assertThat(active).isTrue
                assertThat(inactiveDate).isNull()
                assertThat(cjitCode).isEqualTo("123456789")
                assertThat(accessibleAccess).isEqualTo(AccessibleAccess.WHEELCHAIR_ACCESS)
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(geographicalArea?.description).isEqualTo("West Yorkshire")
                assertThat(localAuthority?.description).isEqualTo("Sheffield City Council")
                assertThat(payrollRegion?.code).isEqualTo("NEY")
                assertThat(courtType.description).isEqualTo("Magistrates Court")
                assertThat(addresses).isEmpty()
                assertThat(phoneNumbers).isEmpty()
                assertThat(emailAddresses).isEmpty()
              }
            }
          }

          @Test
          fun `will create an address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "Castle Street",
                      addressLine2 = null,
                      town = "Sheffield",
                      county = "South Yorkshire",
                      postcode = "S3 8LU",
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
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses[0]) {
                  assertThat(addressLine1).isEqualTo("Castle Street")
                  assertThat(addressLine2).isNull()
                  assertThat(town).isEqualTo("Sheffield")
                  assertThat(county).isEqualTo("South Yorkshire")
                  assertThat(postcode).isEqualTo("S3 8LU")
                  assertThat(country).isEqualTo("England")
                }
              }
            }
          }

          @Test
          fun `will create an email address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  emailAddresses = listOf(
                    LegacyAgencyEmailDto(address = "test.sheffield.mc@justice.gov.uk"),
                  ),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(emailAddresses).hasSize(1)
                with(emailAddresses[0]) {
                  assertThat(value).isEqualTo("test.sheffield.mc@justice.gov.uk")
                }
              }
            }
          }

          @Test
          fun `will create phone numbers, including duplicates`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  emailAddresses = emptyList(),
                  addresses = emptyList(),
                  phoneNumbers = listOf(
                    LegacyAgencyPhoneDto(number = "0114 555 5555"),
                    LegacyAgencyPhoneDto(number = "0114 555 5555"),
                    LegacyAgencyPhoneDto(number = "0114 999 5555"),
                  ),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(phoneNumbers).hasSize(3)
                with(phoneNumbers[0]) {
                  assertThat(value).isEqualTo("0114 555 5555")
                }
                with(phoneNumbers[1]) {
                  assertThat(value).isEqualTo("0114 555 5555")
                }
                with(phoneNumbers[2]) {
                  assertThat(value).isEqualTo("0114 999 5555")
                }
              }
            }
          }
        }
      }

      @Nested
      inner class Update {
        val courtRequest = LegacyAgencyDto(
          agencyType = LegacyAgencyType.COURT,
          name = "Sheffield MC",
          description = "Sheffield Magistrates' Court",
          active = true,
          inactiveDate = null,
          cjitCode = "123456789",
          accessibleAccess = LegacyAccessibleAccess.WHEELCHAIR_ACCESS,
          contact = null,
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = "NEY",
          localAuthorityCode = "00CG",
          courtTypeCode = "MC",
          addresses = listOf(
            LegacyAgencyAddressDto(
              addressLine1 = "Castle Street",
              addressLine2 = null,
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S3 8LU",
              country = "England",
            ),
          ),
          emailAddresses = listOf(LegacyAgencyEmailDto(address = "test.sheffield.mc@justice.gov.uk")),
          phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 5555")),
        )

        lateinit var court: Court

        @BeforeEach
        fun setUp() {
          court = dsl.court(
            courtId = "SHEFMC",
            name = "Sheffield MC",
            description = "Sheffield Magistrates' Court",
            active = true,
            inactiveDate = null,
            courtTypeCode = "MC",
            cjitCode = "123456789",
            areaCode = "52",
            regionCode = "YOHUM",
            geographicalAreaCode = null,
          ) {
            address(
              addressLine1 = "Castle Street",
              addressLine2 = null,
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S3 8LU",
              country = "England",
            )
            email(
              emailAddress = "test.sheffield.mc@justice.gov.uk",
            )
            phoneNumber(
              phoneNumber = "0114 555 5555",
            )
          }
        }

        @Nested
        inner class Validation {
          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency SHEFMC")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency SHEFMC")
          }

          @Test
          fun `court type code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(courtTypeCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ court type not found for agency SHEFMC")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SHEFMC")
          }

          @Test
          fun `local authority code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(localAuthorityCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for agency SHEFMC")
          }

          @Test
          fun `payroll region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(payrollRegionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for agency SHEFMC")
          }

          @Test
          fun `court type code missing`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(courtTypeCode = null))
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              val court = courtRepository.findByIdOrNull("SHEFMC")!!

              with(court) {
                assertThat(courtType.description).isEqualTo("Unknown")
              }
            }
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will update the core court data`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(courtRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01")))
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isTrue

            transactionHelper.runInTransaction {
              val court = courtRepository.findByIdOrNull("SHEFMC")!!

              with(court) {
                assertThat(name).isEqualTo("Sheffield MC")
                assertThat(description).isEqualTo("Sheffield Magistrates' Court")
                assertThat(active).isFalse
                assertThat(inactiveDate).isEqualTo("2026-01-01")
                assertThat(cjitCode).isEqualTo("123456789")
                assertThat(accessibleAccess).isEqualTo(AccessibleAccess.WHEELCHAIR_ACCESS)
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(courtType.description).isEqualTo("Magistrates Court")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(geographicalArea?.description).isEqualTo("West Yorkshire")
                assertThat(localAuthority?.description).isEqualTo("Sheffield City Council")
                assertThat(payrollRegion?.code).isEqualTo("NEY")
              }
            }
          }

          @Test
          fun `will update existing address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "Castle Street",
                      addressLine2 = "City Centre",
                      town = "Sheffield",
                      county = "South Yorkshire",
                      postcode = "S3 8LU",
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
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses[0]) {
                  assertThat(addressLine1).isEqualTo("Castle Street")
                  assertThat(addressLine2).isEqualTo("City Centre")
                  assertThat(town).isEqualTo("Sheffield")
                  assertThat(county).isEqualTo("South Yorkshire")
                  assertThat(postcode).isEqualTo("S3 8LU")
                  assertThat(country).isEqualTo("England")
                }
              }
            }
          }

          @Test
          fun `will remove existing address`() {
            transactionHelper.runInTransaction {
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(addresses).hasSize(1)
              }
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(addresses).hasSize(0)
              }
            }
          }

          @Test
          fun `will update existing address add others`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "Front Entrance",
                      addressLine2 = "Castle Street",
                      town = "Sheffield",
                      county = "South Yorkshire",
                      postcode = "S3 8LU",
                      country = "England",
                    ),
                    LegacyAgencyAddressDto(
                      addressLine1 = "Back Entrance",
                      addressLine2 = "Castle Street",
                      town = "Sheffield",
                      county = "South Yorkshire",
                      postcode = "S3 8LU",
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
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(addresses).hasSize(2)
                with(addresses.first { it.addressLine1 == "Front Entrance" }) {
                  assertThat(addressLine1).isEqualTo("Front Entrance")
                  assertThat(addressLine2).isEqualTo("Castle Street")
                  assertThat(town).isEqualTo("Sheffield")
                  assertThat(county).isEqualTo("South Yorkshire")
                  assertThat(postcode).isEqualTo("S3 8LU")
                  assertThat(country).isEqualTo("England")
                }
                with(addresses.first { it.addressLine1 == "Back Entrance" }) {
                  assertThat(addressLine1).isEqualTo("Back Entrance")
                  assertThat(addressLine2).isEqualTo("Castle Street")
                  assertThat(town).isEqualTo("Sheffield")
                  assertThat(county).isEqualTo("South Yorkshire")
                  assertThat(postcode).isEqualTo("S3 8LU")
                  assertThat(country).isEqualTo("England")
                }
              }
            }
          }

          @Test
          fun `will update an email address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  emailAddresses = listOf(
                    LegacyAgencyEmailDto(address = "test.2.sheffield.mc@justice.gov.uk"),
                  ),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(emailAddresses).hasSize(1)
                with(emailAddresses[0]) {
                  assertThat(value).isEqualTo("test.2.sheffield.mc@justice.gov.uk")
                }
              }
            }
          }

          @Test
          fun `will create and update email addresses`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  emailAddresses = listOf(
                    LegacyAgencyEmailDto(address = "test.sheffield.mc@justice.gov.uk"),
                    LegacyAgencyEmailDto(address = "test.2.sheffield.mc@justice.gov.uk"),
                  ),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(emailAddresses).hasSize(2)
                with(emailAddresses[0]) {
                  assertThat(value).isEqualTo("test.sheffield.mc@justice.gov.uk")
                }
                with(emailAddresses[1]) {
                  assertThat(value).isEqualTo("test.2.sheffield.mc@justice.gov.uk")
                }
              }
            }
          }

          @Test
          fun `will remove email addresses`() {
            transactionHelper.runInTransaction {
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(emailAddresses).hasSize(1)
              }
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  emailAddresses = emptyList(),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(emailAddresses).hasSize(0)
              }
            }
          }

          @Test
          fun `will remove phone numbers`() {
            transactionHelper.runInTransaction {
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(phoneNumbers).hasSize(1)
              }
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  emailAddresses = emptyList(),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(phoneNumbers).hasSize(0)
              }
            }
          }

          @Test
          fun `will update phone numbers`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  emailAddresses = emptyList(),
                  addresses = emptyList(),
                  phoneNumbers = listOf(
                    LegacyAgencyPhoneDto(number = "0114 999 5555"),
                  ),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(phoneNumbers).hasSize(1)
                with(phoneNumbers[0]) {
                  assertThat(value).isEqualTo("0114 999 5555")
                }
              }
            }
          }

          @Test
          fun `will create and update phone numbers`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                courtRequest.copy(
                  emailAddresses = emptyList(),
                  addresses = emptyList(),
                  phoneNumbers = listOf(
                    LegacyAgencyPhoneDto(number = "0114 555 5555"),
                    LegacyAgencyPhoneDto(number = "0114 999 5555"),
                  ),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(courtRepository.findByIdOrNull("SHEFMC")!!) {
                assertThat(phoneNumbers).hasSize(2)
                with(phoneNumbers[0]) {
                  assertThat(value).isEqualTo("0114 555 5555")
                }
                with(phoneNumbers[1]) {
                  assertThat(value).isEqualTo("0114 999 5555")
                }
              }
            }
          }
        }
      }
    }
  }
}
