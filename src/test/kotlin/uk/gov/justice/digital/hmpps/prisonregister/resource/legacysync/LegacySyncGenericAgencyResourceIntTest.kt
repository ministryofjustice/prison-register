package uk.gov.justice.digital.hmpps.prisonregister.resource.legacysync

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.Agency
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class LegacySyncGenericAgencyResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var agencyRepository: AgencyRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @Autowired
  lateinit var dsl: Root

  @MockitoBean
  lateinit var telemetry: TelemetryClient

  companion object {
    fun legacyAgencyDto() = LegacyAgencyDto(
      agencyType = LegacyAgencyType.COURT,
      name = "Sheffield MC",
      description = "Sheffield Magistrates' Court",
      active = true,
      inactiveDate = null,
      contact = null,
      cjitCode = "123456789",
      accessibleAccess = null,
      areaCode = "52",
      regionCode = "YOHUM",
      geographicalAreaCode = null,
      payrollRegionCode = "HS",
      courtTypeCode = "MC",
      addresses = listOf(),
      emailAddresses = listOf(),
      phoneNumbers = listOf(),
    )

    @JvmStatic
    fun legacyAgencyTypes() = LegacyAgencyType.entries
      .filterNot {
        it.name in setOf(
          "PRISON",
          "COURT",
          "HOSPITAL",
          "SECURE_HOSPITAL",
          "APPROVED_PREMISE",
          "POLICE_CUSTODY_SUITE",
          "PROBATION_OFFICE",
        )
      }
  }

  @AfterEach
  fun tearDown() {
    agencyRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /legacy/sync/agency/id/{agencyId}")
  inner class CreateOrUpdateAgency {
    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(legacyAgencyDto())
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(legacyAgencyDto())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/legacy/sync/agency/id/{agencyId}", "SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(legacyAgencyDto())
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class WhenGenericAgency {
      // Uses PROBATION_CRC as a representative unhandled LegacyAgencyType that maps to AgencyType
      val agencyRequest = LegacyAgencyDto(
        agencyType = LegacyAgencyType.PROBATION_CRC,
        name = "Sheffield CRC",
        description = "Sheffield Community Rehabilitation Company",
        active = true,
        inactiveDate = null,
        cjitCode = "123456789",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "HS",
        courtTypeCode = null,
        accessibleAccess = null,
        contact = null,
        addresses = listOf(
          LegacyAgencyAddressDto(
            addressLine1 = "1 Charter Row",
            addressLine2 = null,
            town = "Sheffield",
            county = "South Yorkshire",
            postcode = "S1 3FZ",
            country = "England",
          ),
        ),
        emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.crc@justice.gov.uk")),
        phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 1234")),
      )

      @Nested
      inner class Create {

        @Nested
        inner class Validation {
          @Test
          fun `agency type not mappable to AgencyType is rejected`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(agencyRequest.copy(agencyType = LegacyAgencyType.PRISON))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("PRISON agency type not supported for agency SFCRC")
          }

          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(agencyRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency SFCRC")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(agencyRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency SFCRC")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(agencyRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SFCRC")
          }

          @Test
          fun `payroll region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(agencyRequest.copy(payrollRegionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for agency SFCRC")
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will track telemetry`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(agencyRequest)
              .exchange()
              .expectStatus().isOk

            verify(telemetry).trackEvent("legacy-sync-agency-created", mapOf("agencyId" to "SFCRC"), null)
          }

          @Test
          fun `will create the core agency data`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                agencyRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isFalse

            transactionHelper.runInTransaction {
              val agency = agencyRepository.findByIdOrNull("SFCRC")!!

              with(agency) {
                assertThat(name).isEqualTo("Sheffield CRC")
                assertThat(description).isEqualTo("Sheffield Community Rehabilitation Company")
                assertThat(active).isTrue
                assertThat(agencyType).isEqualTo(AgencyType.PROBATION_CRC)
                assertThat(inactiveDate).isNull()
                assertThat(cjitCode).isEqualTo("123456789")
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(geographicalArea?.description).isEqualTo("West Yorkshire")
                assertThat(payrollRegion?.description).isEqualTo("High Security")
                assertThat(addresses).isEmpty()
                assertThat(phoneNumbers).isEmpty()
                assertThat(emailAddresses).isEmpty()
              }
            }
          }

          @ParameterizedTest
          @MethodSource("uk.gov.justice.digital.hmpps.prisonregister.resource.legacysync.LegacySyncGenericAgencyResourceIntTest#legacyAgencyTypes")
          fun `can create all legacy agency types`(agencyType: LegacyAgencyType) {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "AGY123")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(agencyRequest.copy(agencyType = agencyType))
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isFalse

            val agency = agencyRepository.findByIdOrNull("AGY123")
            assertThat(agency).isNotNull
            assertThat(agency!!.agencyType.name).isEqualTo(agencyType.name)
          }

          @Test
          fun `will create an address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                agencyRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "1 Charter Row",
                      addressLine2 = null,
                      town = "Sheffield",
                      county = "South Yorkshire",
                      postcode = "S1 3FZ",
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
              with(agencyRepository.findByIdOrNull("SFCRC")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses[0]) {
                  assertThat(addressLine1).isEqualTo("1 Charter Row")
                  assertThat(addressLine2).isNull()
                  assertThat(town).isEqualTo("Sheffield")
                  assertThat(county).isEqualTo("South Yorkshire")
                  assertThat(postcode).isEqualTo("S1 3FZ")
                  assertThat(country).isEqualTo("England")
                }
              }
            }
          }

          @Test
          fun `will create an email address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                agencyRequest.copy(
                  emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.crc@justice.gov.uk")),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(agencyRepository.findByIdOrNull("SFCRC")!!) {
                assertThat(emailAddresses).hasSize(1)
                assertThat(emailAddresses[0].value).isEqualTo("sheffield.crc@justice.gov.uk")
              }
            }
          }

          @Test
          fun `will create phone numbers`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                agencyRequest.copy(
                  emailAddresses = emptyList(),
                  addresses = emptyList(),
                  phoneNumbers = listOf(
                    LegacyAgencyPhoneDto(number = "0114 555 1234"),
                    LegacyAgencyPhoneDto(number = "0114 555 5678"),
                  ),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(agencyRepository.findByIdOrNull("SFCRC")!!) {
                assertThat(phoneNumbers).hasSize(2)
                assertThat(phoneNumbers[0].value).isEqualTo("0114 555 1234")
                assertThat(phoneNumbers[1].value).isEqualTo("0114 555 5678")
              }
            }
          }
        }
      }

      @Nested
      inner class Update {
        val updateRequest = LegacyAgencyDto(
          agencyType = LegacyAgencyType.PROBATION_CRC,
          name = "Sheffield CRC",
          description = "Sheffield Community Rehabilitation Company",
          active = true,
          inactiveDate = null,
          cjitCode = "123456789",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = "HS",
          courtTypeCode = null,
          accessibleAccess = null,
          contact = null,
          addresses = listOf(
            LegacyAgencyAddressDto(
              addressLine1 = "1 Charter Row",
              addressLine2 = null,
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S1 3FZ",
              country = "England",
            ),
          ),
          emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.crc@justice.gov.uk")),
          phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 1234")),
        )

        lateinit var agency: Agency

        @BeforeEach
        fun setUp() {
          agency = dsl.agency(
            agencyId = "SFCRC",
            name = "Sheffield CRC",
            description = "Sheffield Community Rehabilitation Company",
            active = true,
            agencyType = AgencyType.PROBATION_CRC,
            inactiveDate = null,
            cjitCode = "123456789",
            areaCode = "52",
            regionCode = "YOHUM",
            geographicalAreaCode = "WYORKS",
            payrollRegionCode = "HS",
          ) {
            address(
              addressLine1 = "1 Charter Row",
              addressLine2 = null,
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S1 3FZ",
              country = "England",
            )
            email(emailAddress = "sheffield.crc@justice.gov.uk")
            phoneNumber(phoneNumber = "0114 555 1234")
          }
        }

        @Nested
        inner class Validation {
          @Test
          fun `agency type not mappable to AgencyType is rejected`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(agencyType = LegacyAgencyType.PRISON))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("PRISON agency type not supported for agency SFCRC")
          }

          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency SFCRC")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency SFCRC")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SFCRC")
          }

          @Test
          fun `payroll region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(payrollRegionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for agency SFCRC")
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will track telemetry`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest)
              .exchange()
              .expectStatus().isOk

            verify(telemetry).trackEvent("legacy-sync-agency-updated", mapOf("agencyId" to "SFCRC"), null)
          }

          @Test
          fun `will update the core agency data`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01")))
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isTrue

            transactionHelper.runInTransaction {
              val updated = agencyRepository.findByIdOrNull("SFCRC")!!

              with(updated) {
                assertThat(name).isEqualTo("Sheffield CRC")
                assertThat(description).isEqualTo("Sheffield Community Rehabilitation Company")
                assertThat(active).isFalse
                assertThat(agencyType).isEqualTo(AgencyType.PROBATION_CRC)
                assertThat(inactiveDate).isEqualTo(LocalDate.parse("2026-01-01"))
                assertThat(cjitCode).isEqualTo("123456789")
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(geographicalArea?.description).isEqualTo("West Yorkshire")
                assertThat(payrollRegion?.description).isEqualTo("High Security")
              }
            }
          }

          @Test
          fun `will update existing address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "1 Charter Row",
                      addressLine2 = "Floor 2",
                      town = "Sheffield",
                      county = "South Yorkshire",
                      postcode = "S1 3FZ",
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
              with(agencyRepository.findByIdOrNull("SFCRC")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses[0]) {
                  assertThat(addressLine1).isEqualTo("1 Charter Row")
                  assertThat(addressLine2).isEqualTo("Floor 2")
                }
              }
            }
          }

          @Test
          fun `will remove existing address`() {
            transactionHelper.runInTransaction {
              assertThat(agencyRepository.findByIdOrNull("SFCRC")!!.addresses).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
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
              assertThat(agencyRepository.findByIdOrNull("SFCRC")!!.addresses).isEmpty()
            }
          }

          @Test
          fun `will update email addresses`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  emailAddresses = listOf(LegacyAgencyEmailDto(address = "new.sheffield.crc@justice.gov.uk")),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(agencyRepository.findByIdOrNull("SFCRC")!!) {
                assertThat(emailAddresses).hasSize(1)
                assertThat(emailAddresses[0].value).isEqualTo("new.sheffield.crc@justice.gov.uk")
              }
            }
          }

          @Test
          fun `will remove email addresses`() {
            transactionHelper.runInTransaction {
              assertThat(agencyRepository.findByIdOrNull("SFCRC")!!.emailAddresses).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
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
              assertThat(agencyRepository.findByIdOrNull("SFCRC")!!.emailAddresses).isEmpty()
            }
          }

          @Test
          fun `will update phone numbers`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 9999")),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(agencyRepository.findByIdOrNull("SFCRC")!!) {
                assertThat(phoneNumbers).hasSize(1)
                assertThat(phoneNumbers[0].value).isEqualTo("0114 555 9999")
              }
            }
          }

          @Test
          fun `will remove phone numbers`() {
            transactionHelper.runInTransaction {
              assertThat(agencyRepository.findByIdOrNull("SFCRC")!!.phoneNumbers).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SFCRC")
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
              assertThat(agencyRepository.findByIdOrNull("SFCRC")!!.phoneNumbers).isEmpty()
            }
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("POST /legacy/migrate/agency/id/{agencyId}")
  inner class MigrateAgency {
    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/legacy/migrate/agency/id/{agencyId}", "SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(legacyAgencyDto())
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/legacy/migrate/agency/id/{agencyId}", "SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(legacyAgencyDto())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.post()
          .uri("/legacy/migrate/agency/id/{agencyId}", "SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .bodyValue(legacyAgencyDto())
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class WhenGenericAgency {

      @Nested
      inner class Created {
        val createRequest = LegacyAgencyDto(
          agencyType = LegacyAgencyType.PROBATION_CRC,
          name = "Sheffield CRC",
          description = "Sheffield Community Rehabilitation Company",
          active = true,
          inactiveDate = null,
          cjitCode = "123456789",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = "HS",
          courtTypeCode = null,
          accessibleAccess = null,
          contact = null,
          addresses = listOf(
            LegacyAgencyAddressDto(
              addressLine1 = "1 Charter Row",
              addressLine2 = null,
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S1 3FZ",
              country = "England",
            ),
          ),
          emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.crc@justice.gov.uk")),
          phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 1234")),
        )

        @Test
        fun `will track telemetry`() {
          webTestClient.post()
            .uri("/legacy/migrate/agency/id/{agencyId}", "SFCRC")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isOk

          verify(telemetry).trackEvent("legacy-migration-agency-created", mapOf("agencyId" to "SFCRC"), null)
        }
      }

      @Nested
      inner class Updated {
        val updateRequest = LegacyAgencyDto(
          agencyType = LegacyAgencyType.PROBATION_CRC,
          name = "Sheffield CRC",
          description = "Sheffield Community Rehabilitation Company",
          active = true,
          inactiveDate = null,
          cjitCode = "123456789",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = "HS",
          courtTypeCode = null,
          accessibleAccess = null,
          contact = null,
          addresses = listOf(
            LegacyAgencyAddressDto(
              addressLine1 = "1 Charter Row",
              addressLine2 = null,
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S1 3FZ",
              country = "England",
            ),
          ),
          emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.crc@justice.gov.uk")),
          phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 1234")),
        )

        @BeforeEach
        fun setUp() {
          dsl.agency(
            agencyId = "SFCRC",
            name = "Sheffield CRC",
            description = "Sheffield Community Rehabilitation Company",
            active = true,
            agencyType = AgencyType.PROBATION_CRC,
            inactiveDate = null,
            cjitCode = "123456789",
            areaCode = "52",
            regionCode = "YOHUM",
            geographicalAreaCode = "WYORKS",
            payrollRegionCode = "HS",
          ) {
            address(
              addressLine1 = "1 Charter Row",
              addressLine2 = null,
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S1 3FZ",
              country = "England",
            )
            email(emailAddress = "sheffield.crc@justice.gov.uk")
            phoneNumber(phoneNumber = "0114 555 1234")
          }
        }

        @Test
        fun `will track telemetry`() {
          webTestClient.post()
            .uri("/legacy/migrate/agency/id/{agencyId}", "SFCRC")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk

          verify(telemetry).trackEvent("legacy-migration-agency-updated", mapOf("agencyId" to "SFCRC"), null)
        }
      }
    }
  }
}
