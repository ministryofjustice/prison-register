package uk.gov.justice.digital.hmpps.prisonregister.resource.legacysync

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetailsRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class LegacySyncPrisonResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var prisonRepository: PrisonRepository

  @Autowired
  lateinit var contactDetailsRepository: ContactDetailsRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @Autowired
  lateinit var dsl: Root

  @MockitoBean
  lateinit var telemetry: TelemetryClient

  companion object {
    fun legacyAgencyDto() = LegacyAgencyDto(
      agencyType = LegacyAgencyType.PRISON,
      name = "Brixton (HMP)",
      description = "HMP Brixton",
      active = true,
      inactiveDate = null,
      contact = null,
      cjitCode = "123456789",
      accessibleAccess = null,
      areaCode = "52",
      regionCode = "YOHUM",
      geographicalAreaCode = "52",
      payrollRegionCode = "HS",
      localAuthorityCode = "00CG",
      courtTypeCode = "MC",
      addresses = listOf(),
      emailAddresses = listOf(),
      phoneNumbers = listOf(),
    )
  }

  @AfterEach
  fun tearDown() {
    contactDetailsRepository.findAll()
      .filter { it.prisonId == "BXI" }
      .forEach(contactDetailsRepository::delete)
    if (prisonRepository.existsById("BXI")) prisonRepository.deleteById("BXI")
  }

  @Nested
  @DisplayName("POST /legacy/sync/prison/id/{agencyId}")
  inner class CreateOrUpdateOtherAgency {
    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.post()
          .uri("/legacy/sync/prison/id/{agencyId}", "BXI")
          .accept(MediaType.APPLICATION_JSON)
          .bodyValue(legacyAgencyDto())
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.post()
          .uri("/legacy/sync/prison/id/{agencyId}", "BXI")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(legacyAgencyDto())
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class WhenPrison {
      val agencyRequest = LegacyAgencyDto(
        agencyType = LegacyAgencyType.PRISON,
        name = "Brixton (HMP)",
        description = "HMP Brixton",
        active = true,
        inactiveDate = null,
        cjitCode = "123456789",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "HS",
        courtTypeCode = null,
        localAuthorityCode = "00CG",
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
        inner class HappyPath {

          @Test
          fun `not supported yet`() {
            webTestClient.post()
              .uri("/legacy/sync/prison/id/{agencyId}", "ZZI")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(agencyRequest)
              .exchange()
              .expectStatus().is5xxServerError
          }
        }
      }

      @Nested
      inner class Update {
        val updateRequest = LegacyAgencyDto(
          agencyType = LegacyAgencyType.PRISON,
          name = "Brixton New (HMP)",
          description = "HMP Brixton",
          active = false,
          inactiveDate = LocalDate.parse("2026-01-01"),
          cjitCode = "123456789",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = "HS",
          localAuthorityCode = "00CG",
          courtTypeCode = null,
          accessibleAccess = null,
          contact = null,
          addresses = listOf(
            LegacyAgencyAddressDto(
              addressLine1 = "99 Charter Row",
              addressLine2 = null,
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S1 3FZ",
              country = "England",
            ),
          ),
          emailAddresses = listOf(LegacyAgencyEmailDto(address = "briton.new@justice.gov.uk")),
          phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 111 5555")),
        )

        lateinit var prison: Prison

        @BeforeEach
        fun setUp() {
          prison = dsl.prison(
            prisonId = "BXI",
            name = "Brixton Old (HMP)",
            description = null,
            active = false,
            inactiveDate = null,
            cjitCode = null,
            areaCode = null,
            regionCode = null,
            geographicalAreaCode = null,
            payrollRegionCode = null,
          ) {
            address(
              addressLine1 = "1 Charter Row",
              addressLine2 = null,
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S1 3FZ",
              country = "England",
            )
            email(emailAddress = "brixton.old@justice.gov.uk")
            phoneNumber(phoneNumber = "0114 222 5555")
          }
        }

        @Nested
        inner class Validation {

          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/prison/id/{agencyId}", "BXI")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency BXI")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/prison/id/{agencyId}", "BXI")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency BXI")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/prison/id/{agencyId}", "BXI")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency BXI")
          }

          @Test
          fun `payroll region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/prison/id/{agencyId}", "BXI")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(payrollRegionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for agency BXI")
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will track telemetry`() {
            webTestClient.post()
              .uri("/legacy/sync/prison/id/{agencyId}", "BXI")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest)
              .exchange()
              .expectStatus().isOk

            verify(telemetry).trackEvent("legacy-sync-prison-updated", mapOf("agencyId" to "BXI"), null)
          }

          @Test
          fun `will update some core agency data`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/prison/id/{agencyId}", "BXI")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01")))
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isTrue

            transactionHelper.runInTransaction {
              val updated = prisonRepository.findByIdOrNull("BXI")!!

              with(updated) {
                // unchanged
                assertThat(name).isEqualTo("Brixton Old (HMP)")

                assertThat(description).isEqualTo("HMP Brixton")
                assertThat(active).isFalse
                assertThat(inactiveDate).isEqualTo(LocalDate.parse("2026-01-01"))
                assertThat(cjitCode).isEqualTo("123456789")
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(geographicalArea?.description).isEqualTo("West Yorkshire")
                assertThat(payrollRegion?.description).isEqualTo("High Security")
                assertThat(localAuthority?.description).isEqualTo("Sheffield City Council")
              }
            }
          }

          @Test
          fun `will not update existing address yet`() {
            webTestClient.post()
              .uri("/legacy/sync/prison/id/{agencyId}", "BXI")
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
              with(prisonRepository.findByIdOrNull("BXI")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses.toList()[0]) {
                  assertThat(addressLine1).isEqualTo("1 Charter Row")
                }
              }
            }
          }

          @Test
          fun `will not update existing prison email yet`() {
            webTestClient.post()
              .uri("/legacy/sync/prison/id/{agencyId}", "BXI")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  emailAddresses = listOf(
                    LegacyAgencyEmailDto(
                      address = "test@example.com",
                    ),
                  ),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(contactDetailsRepository.getByPrisonIdAndType("BXI", DepartmentType.PRISON)!!) {
                assertThat(emailAddress?.value).isEqualTo("brixton.old@justice.gov.uk")
              }
            }
          }

          @Test
          fun `will not update existing prison phone yet`() {
            webTestClient.post()
              .uri("/legacy/sync/prison/id/{agencyId}", "BXI")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  phoneNumbers = listOf(
                    LegacyAgencyPhoneDto(
                      number = "0114 555 2222",
                    ),
                  ),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(contactDetailsRepository.getByPrisonIdAndType("BXI", DepartmentType.PRISON)!!) {
                assertThat(phoneNumber?.value).isEqualTo("0114 222 5555")
              }
            }
          }
        }
      }
    }
  }
}
