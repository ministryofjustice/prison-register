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
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremise
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremiseRepository
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class LegacySyncApprovedPremiseResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var approvedPremiseRepository: ApprovedPremiseRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @Autowired
  lateinit var dsl: Root

  @MockitoBean
  lateinit var telemetry: TelemetryClient

  @AfterEach
  fun tearDown() {
    approvedPremiseRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /legacy/sync/agency/id/{agencyId}")
  inner class CreateOrUpdateAgency {

    @Nested
    inner class WhenApprovedPremise {
      val approvedPremiseRequest = LegacyAgencyDto(
        agencyType = LegacyAgencyType.APPROVED_PREMISE,
        name = "Sheffield Approved Premises",
        description = "Sheffield City Centre Approved Premises",
        active = true,
        inactiveDate = null,
        contact = "Gemma Smith",
        cjitCode = "123456789",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        localAuthorityCode = "00CG",
        payrollRegionCode = "NEY",
        courtTypeCode = null,
        accessibleAccess = null,
        addresses = listOf(
          LegacyAgencyAddressDto(
            addressLine1 = "14 West Bar",
            addressLine2 = "City Centre",
            town = "Sheffield",
            county = "South Yorkshire",
            postcode = "S3 8PT",
            country = "England",
          ),
        ),
        emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.approvedpremises@justice.gov.uk")),
        phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 8888")),
      )

      @Nested
      inner class Create {

        @Nested
        inner class Validation {
          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(approvedPremiseRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency SHEFAP")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(approvedPremiseRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency SHEFAP")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(approvedPremiseRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SHEFAP")
          }

          @Test
          fun `local authority code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(approvedPremiseRequest.copy(localAuthorityCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for agency SHEFAP")
          }

          @Test
          fun `payroll region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(approvedPremiseRequest.copy(payrollRegionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for agency SHEFAP")
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will create the core approved premise data`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                approvedPremiseRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isFalse

            transactionHelper.runInTransaction {
              val approvedPremise = approvedPremiseRepository.findByIdOrNull("SHEFAP")!!

              with(approvedPremise) {
                assertThat(name).isEqualTo("Sheffield Approved Premises")
                assertThat(description).isEqualTo("Sheffield City Centre Approved Premises")
                assertThat(active).isTrue
                assertThat(inactiveDate).isNull()
                assertThat(cjitCode).isEqualTo("123456789")
                assertThat(contact).isEqualTo("Gemma Smith")
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(geographicalArea?.description).isEqualTo("West Yorkshire")
                assertThat(payrollRegion?.code).isEqualTo("NEY")
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
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                approvedPremiseRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "14 West Bar",
                      addressLine2 = "City Centre",
                      town = "Sheffield",
                      county = "South Yorkshire",
                      postcode = "S3 8PT",
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
              with(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses[0]) {
                  assertThat(addressLine1).isEqualTo("14 West Bar")
                  assertThat(addressLine2).isEqualTo("City Centre")
                  assertThat(town).isEqualTo("Sheffield")
                  assertThat(county).isEqualTo("South Yorkshire")
                  assertThat(postcode).isEqualTo("S3 8PT")
                  assertThat(country).isEqualTo("England")
                }
              }
            }
          }

          @Test
          fun `will create an email address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                approvedPremiseRequest.copy(
                  emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.approvedpremises@justice.gov.uk")),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!) {
                assertThat(emailAddresses).hasSize(1)
                assertThat(emailAddresses[0].value).isEqualTo("sheffield.approvedpremises@justice.gov.uk")
              }
            }
          }

          @Test
          fun `will create phone numbers`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                approvedPremiseRequest.copy(
                  emailAddresses = emptyList(),
                  addresses = emptyList(),
                  phoneNumbers = listOf(
                    LegacyAgencyPhoneDto(number = "0114 555 8888"),
                    LegacyAgencyPhoneDto(number = "0114 999 8888"),
                  ),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!) {
                assertThat(phoneNumbers).hasSize(2)
                assertThat(phoneNumbers[0].value).isEqualTo("0114 555 8888")
                assertThat(phoneNumbers[1].value).isEqualTo("0114 999 8888")
              }
            }
          }
        }
      }

      @Nested
      inner class Update {
        val updateRequest = LegacyAgencyDto(
          agencyType = LegacyAgencyType.APPROVED_PREMISE,
          name = "Sheffield Approved Premises",
          description = "Sheffield City Centre Approved Premises",
          active = true,
          inactiveDate = null,
          cjitCode = "123456789",
          areaCode = "52",
          regionCode = "YOHUM",
          contact = "Gemma Smith",
          geographicalAreaCode = "WYORKS",
          localAuthorityCode = "00CG",
          payrollRegionCode = "NEY",
          courtTypeCode = null,
          accessibleAccess = null,
          addresses = listOf(
            LegacyAgencyAddressDto(
              addressLine1 = "14 West Bar",
              addressLine2 = "City Centre",
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S3 8PT",
              country = "England",
            ),
          ),
          emailAddresses = listOf(LegacyAgencyEmailDto(address = "sheffield.approvedpremises@justice.gov.uk")),
          phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 555 8888")),
        )

        lateinit var approvedPremise: ApprovedPremise

        @BeforeEach
        fun setUp() {
          approvedPremise = dsl.approvedPremise(
            approvedPremiseId = "SHEFAP",
            name = "Sheffield Approved Premises",
            description = "Sheffield City Centre Approved Premises",
            active = true,
            inactiveDate = null,
            contact = "John Jones",
            cjitCode = "123456789",
            areaCode = "52",
            regionCode = "YOHUM",
            geographicalAreaCode = "WYORKS",
            localAuthorityCode = "00CF",
          ) {
            address(
              addressLine1 = "14 West Bar",
              addressLine2 = "City Centre",
              town = "Sheffield",
              county = "South Yorkshire",
              postcode = "S3 8PT",
              country = "England",
            )
            email(emailAddress = "sheffield.approvedpremises@justice.gov.uk")
            phoneNumber(phoneNumber = "0114 555 8888")
          }
        }

        @Nested
        inner class Validation {
          @Test
          fun `area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(areaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ area code not found for agency SHEFAP")
          }

          @Test
          fun `region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(regionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ region code not found for agency SHEFAP")
          }

          @Test
          fun `geographical area code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(geographicalAreaCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SHEFAP")
          }

          @Test
          fun `local authority code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(localAuthorityCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ local authority code not found for agency SHEFAP")
          }

          @Test
          fun `payroll region code is not valid`() {
            val errorResponse: ErrorResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(payrollRegionCode = "ZZZ"))
              .exchange()
              .expectStatus().isBadRequest.expectBodyResponse()

            assertThat(errorResponse.developerMessage).isEqualTo("ZZZ payroll region code not found for agency SHEFAP")
          }
        }

        @Nested
        inner class HappyPath {

          @Test
          fun `will update the core approved premise data`() {
            val response: LegacyAgencyResponse = webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(updateRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01"), localAuthorityCode = "00CG"))
              .exchange()
              .expectStatus().isOk.expectBodyResponse()

            assertThat(response.updated).isTrue

            transactionHelper.runInTransaction {
              val updated = approvedPremiseRepository.findByIdOrNull("SHEFAP")!!

              with(updated) {
                assertThat(name).isEqualTo("Sheffield Approved Premises")
                assertThat(description).isEqualTo("Sheffield City Centre Approved Premises")
                assertThat(active).isFalse
                assertThat(inactiveDate).isEqualTo(LocalDate.parse("2026-01-01"))
                assertThat(cjitCode).isEqualTo("123456789")
                assertThat(contact).isEqualTo("Gemma Smith")
                assertThat(area?.description).isEqualTo("South Yorkshire")
                assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
                assertThat(geographicalArea?.description).isEqualTo("West Yorkshire")
                assertThat(payrollRegion?.code).isEqualTo("NEY")
                assertThat(localAuthority?.description).isEqualTo("Sheffield City Council")
              }
            }
          }

          @Test
          fun `will update existing address`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = listOf(
                    LegacyAgencyAddressDto(
                      addressLine1 = "14 West Bar",
                      addressLine2 = "Floor 3",
                      town = "Sheffield",
                      county = "South Yorkshire",
                      postcode = "S3 8PT",
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
              with(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!) {
                assertThat(addresses).hasSize(1)
                with(addresses[0]) {
                  assertThat(addressLine1).isEqualTo("14 West Bar")
                  assertThat(addressLine2).isEqualTo("Floor 3")
                }
              }
            }
          }

          @Test
          fun `will remove existing address`() {
            transactionHelper.runInTransaction {
              assertThat(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!.addresses).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
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
              assertThat(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!.addresses).isEmpty()
            }
          }

          @Test
          fun `will update email addresses`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  emailAddresses = listOf(LegacyAgencyEmailDto(address = "new.sheffield.ap@justice.gov.uk")),
                  addresses = emptyList(),
                  phoneNumbers = emptyList(),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!) {
                assertThat(emailAddresses).hasSize(1)
                assertThat(emailAddresses[0].value).isEqualTo("new.sheffield.ap@justice.gov.uk")
              }
            }
          }

          @Test
          fun `will remove email addresses`() {
            transactionHelper.runInTransaction {
              assertThat(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!.emailAddresses).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
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
              assertThat(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!.emailAddresses).isEmpty()
            }
          }

          @Test
          fun `will update phone numbers`() {
            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
              .accept(MediaType.APPLICATION_JSON)
              .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
              .bodyValue(
                updateRequest.copy(
                  addresses = emptyList(),
                  emailAddresses = emptyList(),
                  phoneNumbers = listOf(LegacyAgencyPhoneDto(number = "0114 999 8888")),
                ),
              )
              .exchange()
              .expectStatus().isOk

            transactionHelper.runInTransaction {
              with(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!) {
                assertThat(phoneNumbers).hasSize(1)
                assertThat(phoneNumbers[0].value).isEqualTo("0114 999 8888")
              }
            }
          }

          @Test
          fun `will remove phone numbers`() {
            transactionHelper.runInTransaction {
              assertThat(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!.phoneNumbers).hasSize(1)
            }

            webTestClient.post()
              .uri("/legacy/sync/agency/id/{agencyId}", "SHEFAP")
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
              assertThat(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!.phoneNumbers).isEmpty()
            }
          }
        }
      }
    }
  }
}
