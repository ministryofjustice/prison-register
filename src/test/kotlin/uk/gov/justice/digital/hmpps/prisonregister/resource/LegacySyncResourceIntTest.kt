package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremise
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremiseRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Court
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Hospital
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuite
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOffice
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class LegacySyncResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var courtRepository: CourtRepository

  @Autowired
  lateinit var hospitalRepository: HospitalRepository

  @Autowired
  lateinit var probationOfficeRepository: ProbationOfficeRepository

  @Autowired
  lateinit var approvedPremiseRepository: ApprovedPremiseRepository

  @Autowired
  lateinit var policeCustodySuiteRepository: PoliceCustodySuiteRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @Autowired
  lateinit var dsl: Root

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
  }

  @AfterEach
  fun tearDown() {
    courtRepository.deleteAll()
    hospitalRepository.deleteAll()
    probationOfficeRepository.deleteAll()
    approvedPremiseRepository.deleteAll()
    policeCustodySuiteRepository.deleteAll()
  }

  @Nested
  inner class Security {
    @Test
    fun `requires a valid authentication token`() {
      webTestClient.post()
        .uri("/sync/agency/id/{agencyId}", "SHEFMC")
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(legacyAgencyDto())
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      webTestClient.post()
        .uri("/sync/agency/id/{agencyId}", "SHEFMC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("BANANAS")))
        .bodyValue(legacyAgencyDto())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `allowed with correct role`() {
      webTestClient.post()
        .uri("/sync/agency/id/{agencyId}", "SHEFMC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
        .bodyValue(legacyAgencyDto())
        .exchange()
        .expectStatus().isOk
    }
  }

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
        accessibleAccess = null,
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = null,
        payrollRegionCode = null,
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(courtRequest.copy(courtTypeCode = "ZZZ"))
            .exchange()
            .expectStatus().isBadRequest.expectBodyResponse()

          assertThat(errorResponse.developerMessage).isEqualTo("ZZZ court type not found for agency SHEFMC")
        }

        @Test
        fun `court type code missing`() {
          val errorResponse: ErrorResponse = webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(courtRequest.copy(courtTypeCode = null))
            .exchange()
            .expectStatus().isBadRequest.expectBodyResponse()

          assertThat(errorResponse.developerMessage).isEqualTo("null court type not found for agency SHEFMC")
        }
      }

      @Nested
      inner class HappyPath {

        @Test
        fun `will create the core court data`() {
          val response: LegacyAgencyResponse = webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
              assertThat(area?.description).isEqualTo("South Yorkshire")
              assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
        fun `will create phone numbers`() {
          webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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

    @Nested
    inner class Update {
      val courtRequest = LegacyAgencyDto(
        agencyType = LegacyAgencyType.COURT,
        name = "Sheffield MC",
        description = "Sheffield Magistrates' Court",
        active = true,
        inactiveDate = null,
        cjitCode = "123456789",
        accessibleAccess = null,
        contact = null,
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = null,
        payrollRegionCode = null,
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(courtRequest.copy(courtTypeCode = "ZZZ"))
            .exchange()
            .expectStatus().isBadRequest.expectBodyResponse()

          assertThat(errorResponse.developerMessage).isEqualTo("ZZZ court type not found for agency SHEFMC")
        }

        @Test
        fun `court type code missing`() {
          val errorResponse: ErrorResponse = webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(courtRequest.copy(courtTypeCode = null))
            .exchange()
            .expectStatus().isBadRequest.expectBodyResponse()

          assertThat(errorResponse.developerMessage).isEqualTo("null court type not found for agency SHEFMC")
        }
      }

      @Nested
      inner class HappyPath {

        @Test
        fun `will update the core court data`() {
          val response: LegacyAgencyResponse = webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
              assertThat(area?.description).isEqualTo("South Yorkshire")
              assertThat(region?.description).isEqualTo("Yorkshire & Humberside")
              assertThat(courtType.description).isEqualTo("Magistrates Court")
            }
          }
        }

        @Test
        fun `will update existing address`() {
          webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFMC")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "BRDMR")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01"), accessibleAccess = LegacyAccessibleAccess.BY_ARRANGEMENT_ONLY))
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest.copy(addresses = emptyList(), emailAddresses = emptyList(), phoneNumbers = emptyList()))
            .exchange()
            .expectStatus().isOk

          transactionHelper.runInTransaction {
            assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")!!.addresses).isEmpty()
          }
        }

        @Test
        fun `will update email addresses`() {
          webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest.copy(addresses = emptyList(), emailAddresses = emptyList(), phoneNumbers = emptyList()))
            .exchange()
            .expectStatus().isOk

          transactionHelper.runInTransaction {
            assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")!!.emailAddresses).isEmpty()
          }
        }

        @Test
        fun `will update phone numbers`() {
          webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPB")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest.copy(addresses = emptyList(), emailAddresses = emptyList(), phoneNumbers = emptyList()))
            .exchange()
            .expectStatus().isOk

          transactionHelper.runInTransaction {
            assertThat(probationOfficeRepository.findByIdOrNull("SHEFPB")!!.phoneNumbers).isEmpty()
          }
        }
      }
    }
  }

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
      payrollRegionCode = null,
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
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(approvedPremiseRequest.copy(geographicalAreaCode = "ZZZ"))
            .exchange()
            .expectStatus().isBadRequest.expectBodyResponse()

          assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SHEFAP")
        }
      }

      @Nested
      inner class HappyPath {

        @Test
        fun `will create the core approved premise data`() {
          val response: LegacyAgencyResponse = webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
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
              assertThat(addresses).isEmpty()
              assertThat(phoneNumbers).isEmpty()
              assertThat(emailAddresses).isEmpty()
            }
          }
        }

        @Test
        fun `will create an address`() {
          webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
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
        payrollRegionCode = null,
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
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest.copy(geographicalAreaCode = "ZZZ"))
            .exchange()
            .expectStatus().isBadRequest.expectBodyResponse()

          assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SHEFAP")
        }
      }

      @Nested
      inner class HappyPath {

        @Test
        fun `will update the core approved premise data`() {
          val response: LegacyAgencyResponse = webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest.copy(active = false, inactiveDate = LocalDate.parse("2026-01-01")))
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
            }
          }
        }

        @Test
        fun `will update existing address`() {
          webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest.copy(addresses = emptyList(), emailAddresses = emptyList(), phoneNumbers = emptyList()))
            .exchange()
            .expectStatus().isOk

          transactionHelper.runInTransaction {
            assertThat(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!.addresses).isEmpty()
          }
        }

        @Test
        fun `will update email addresses`() {
          webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest.copy(addresses = emptyList(), emailAddresses = emptyList(), phoneNumbers = emptyList()))
            .exchange()
            .expectStatus().isOk

          transactionHelper.runInTransaction {
            assertThat(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!.emailAddresses).isEmpty()
          }
        }

        @Test
        fun `will update phone numbers`() {
          webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFAP")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest.copy(addresses = emptyList(), emailAddresses = emptyList(), phoneNumbers = emptyList()))
            .exchange()
            .expectStatus().isOk

          transactionHelper.runInTransaction {
            assertThat(approvedPremiseRepository.findByIdOrNull("SHEFAP")!!.phoneNumbers).isEmpty()
          }
        }
      }
    }
  }

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
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(policeCustodySuiteRequest.copy(geographicalAreaCode = "ZZZ"))
            .exchange()
            .expectStatus().isBadRequest.expectBodyResponse()

          assertThat(errorResponse.developerMessage).isEqualTo("ZZZ geographical area code not found for agency SHEFPS")
        }
      }

      @Nested
      inner class HappyPath {

        @Test
        fun `will create the core police custody suite data`() {
          val response: LegacyAgencyResponse = webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
              assertThat(addresses).isEmpty()
              assertThat(phoneNumbers).isEmpty()
              assertThat(emailAddresses).isEmpty()
            }
          }
        }

        @Test
        fun `will create an address`() {
          webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
            }
          }
        }

        @Test
        fun `will update existing address`() {
          webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest.copy(addresses = emptyList(), emailAddresses = emptyList(), phoneNumbers = emptyList()))
            .exchange()
            .expectStatus().isOk

          transactionHelper.runInTransaction {
            assertThat(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!.addresses).isEmpty()
          }
        }

        @Test
        fun `will update email addresses`() {
          webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest.copy(addresses = emptyList(), emailAddresses = emptyList(), phoneNumbers = emptyList()))
            .exchange()
            .expectStatus().isOk

          transactionHelper.runInTransaction {
            assertThat(policeCustodySuiteRepository.findByIdOrNull("SHEFPS")!!.emailAddresses).isEmpty()
          }
        }

        @Test
        fun `will update phone numbers`() {
          webTestClient.post()
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
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
            .uri("/sync/agency/id/{agencyId}", "SHEFPS")
            .accept(MediaType.APPLICATION_JSON)
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
            .bodyValue(updateRequest.copy(addresses = emptyList(), emailAddresses = emptyList(), phoneNumbers = emptyList()))
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
