package uk.gov.justice.digital.hmpps.prisonregister.resource.legacysync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremiseRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.AgencyIdsResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.LegacyAgencyType

class LegacySyncAgencyReconciliationResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var agencyRepository: AgencyRepository

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
  lateinit var dsl: Root

  @AfterEach
  fun tearDown() {
    courtRepository.deleteAll()
    hospitalRepository.deleteAll()
    probationOfficeRepository.deleteAll()
    approvedPremiseRepository.deleteAll()
    policeCustodySuiteRepository.deleteAll()
    agencyRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /legacy/reconciliation/ids/all")
  inner class GetAllAgencyIds {

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/legacy/reconciliation/ids/all")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/legacy/reconciliation/ids/all")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/legacy/reconciliation/ids/all")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      @BeforeEach
      fun setUp() {
        dsl.court(courtId = "RECCRT", name = "Reconciliation Court") { }
        dsl.hospital(hospitalId = "RECHSP", name = "Reconciliation Hospital", highSecurity = false) { }
        dsl.probationOffice(probationOfficeId = "RECPBO", name = "Reconciliation Probation Office") { }
        dsl.approvedPremise(approvedPremiseId = "RECAPR", name = "Reconciliation Approved Premise") { }
        dsl.policeCustodySuite(policeCustodySuiteId = "RECPCS", name = "Reconciliation Police Custody Suite") { }
        dsl.agency(agencyId = "RECAGY", name = "Reconciliation Agency") { }
      }

      @Test
      fun `returns IDs of all non-prison agency types`() {
        val response: AgencyIdsResponse = webTestClient.get()
          .uri("/legacy/reconciliation/ids/all")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        val ids = response.agencyIds.map { it.agencyId }
        assertThat(ids).contains("RECCRT", "RECHSP", "RECPBO", "RECAPR", "RECPCS", "RECAGY")
      }

      @Test
      fun `returns an empty list when no agencies exist`() {
        courtRepository.deleteAll()
        hospitalRepository.deleteAll()
        probationOfficeRepository.deleteAll()
        approvedPremiseRepository.deleteAll()
        policeCustodySuiteRepository.deleteAll()
        agencyRepository.deleteAll()

        val response: AgencyIdsResponse = webTestClient.get()
          .uri("/legacy/reconciliation/ids/all")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyIds).isEmpty()
      }
    }
  }

  @Nested
  @DisplayName("GET /legacy/reconciliation/{agencyId}")
  inner class GetAgencyDetails {

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/legacy/reconciliation/RECDET")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/legacy/reconciliation/RECDET")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns details for a court`() {
        dsl.court(
          courtId = "SHEFMC",
          name = "Sheffield MC",
          description = "Sheffield Magistrates' Court",
          cjitCode = "123456789",
          areaCode = "52",
          regionCode = "YOHUM",
          courtTypeCode = "MC",
          geographicalAreaCode = "WYORKS",
        ) {
          address(addressLine1 = "Castle Street", town = "Sheffield", county = "South Yorkshire", postcode = "S3 8LU", country = "England")
          email(emailAddress = "test.sheffield.mc@justice.gov.uk")
          phoneNumber(phoneNumber = "0114 555 5555")
        }

        val response: LegacyAgencyDto = webTestClient.get()
          .uri("/legacy/reconciliation/SHEFMC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyType).isEqualTo(LegacyAgencyType.COURT)
        assertThat(response.name).isEqualTo("Sheffield MC")
        assertThat(response.description).isEqualTo("Sheffield Magistrates' Court")
        assertThat(response.active).isTrue
        assertThat(response.cjitCode).isEqualTo("123456789")
        assertThat(response.areaCode).isEqualTo("52")
        assertThat(response.regionCode).isEqualTo("YOHUM")
        assertThat(response.geographicalAreaCode).isEqualTo("WYORKS")
        assertThat(response.courtTypeCode).isEqualTo("MC")
        assertThat(response.addresses).hasSize(1)
        with(response.addresses[0]) {
          assertThat(addressLine1).isEqualTo("Castle Street")
          assertThat(town).isEqualTo("Sheffield")
          assertThat(county).isEqualTo("South Yorkshire")
          assertThat(postcode).isEqualTo("S3 8LU")
          assertThat(country).isEqualTo("England")
        }
        assertThat(response.emailAddresses).hasSize(1)
        assertThat(response.emailAddresses[0].address).isEqualTo("test.sheffield.mc@justice.gov.uk")
        assertThat(response.phoneNumbers).hasSize(1)
        assertThat(response.phoneNumbers[0].number).isEqualTo("0114 555 5555")
      }

      @Test
      fun `returns details for a hospital`() {
        dsl.hospital(
          hospitalId = "BRDMR",
          name = "Broadmoor Hospital",
          description = "Broadmoor High Security Hospital",
          highSecurity = false,
          cjitCode = "123456789",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = "HS",
          localAuthorityCode = "00CG",
        ) {
          address(addressLine1 = "Crowthorne", town = "Berkshire", county = "Berkshire", postcode = "RG45 7EG", country = "England")
          phoneNumber(phoneNumber = "01344 773111")
        }

        val response: LegacyAgencyDto = webTestClient.get()
          .uri("/legacy/reconciliation/BRDMR")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyType).isEqualTo(LegacyAgencyType.HOSPITAL)
        assertThat(response.name).isEqualTo("Broadmoor Hospital")
        assertThat(response.areaCode).isEqualTo("52")
        assertThat(response.regionCode).isEqualTo("YOHUM")
        assertThat(response.geographicalAreaCode).isEqualTo("WYORKS")
        assertThat(response.payrollRegionCode).isEqualTo("HS")
        assertThat(response.localAuthorityCode).isEqualTo("00CG")
        assertThat(response.addresses).hasSize(1)
        with(response.addresses[0]) {
          assertThat(addressLine1).isEqualTo("Crowthorne")
          assertThat(postcode).isEqualTo("RG45 7EG")
        }
        assertThat(response.emailAddresses).isEmpty()
        assertThat(response.phoneNumbers).hasSize(1)
        assertThat(response.phoneNumbers[0].number).isEqualTo("01344 773111")
      }

      @Test
      fun `returns SECURE_HOSPITAL type for a high-security hospital`() {
        dsl.hospital(
          hospitalId = "BRDSH",
          name = "Broadmoor Secure Hospital",
          highSecurity = true,
        ) { }

        val response: LegacyAgencyDto = webTestClient.get()
          .uri("/legacy/reconciliation/BRDSH")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyType).isEqualTo(LegacyAgencyType.SECURE_HOSPITAL)
        assertThat(response.name).isEqualTo("Broadmoor Secure Hospital")
      }

      @Test
      fun `returns details for a probation office`() {
        dsl.probationOffice(
          probationOfficeId = "SHEFPB",
          name = "Sheffield Probation Office",
          description = "Sheffield City Centre Probation Office",
          cjitCode = "123456789",
          areaCode = "52",
          subareaCode = "SHEFF",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          localAuthorityCode = "00CG",
          accessibleAccess = AccessibleAccess.WHEELCHAIR_ACCESS,
        ) {
          address(addressLine1 = "Probation House, 31 High Street", addressLine2 = "City Centre", town = "Sheffield", county = "South Yorkshire", postcode = "S1 3GG", country = "England")
          email(emailAddress = "sheffield.probation@justice.gov.uk")
          phoneNumber(phoneNumber = "0114 555 7777")
        }

        val response: LegacyAgencyDto = webTestClient.get()
          .uri("/legacy/reconciliation/SHEFPB")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyType).isEqualTo(LegacyAgencyType.PROBATION_OFFICE)
        assertThat(response.name).isEqualTo("Sheffield Probation Office")
        assertThat(response.description).isEqualTo("Sheffield City Centre Probation Office")
        assertThat(response.cjitCode).isEqualTo("123456789")
        assertThat(response.areaCode).isEqualTo("52")
        assertThat(response.subareaCode).isEqualTo("SHEFF")
        assertThat(response.regionCode).isEqualTo("YOHUM")
        assertThat(response.geographicalAreaCode).isEqualTo("WYORKS")
        assertThat(response.localAuthorityCode).isEqualTo("00CG")
        assertThat(response.accessibleAccess).isEqualTo(LegacyAccessibleAccess.WHEELCHAIR_ACCESS)
        assertThat(response.addresses).hasSize(1)
        with(response.addresses[0]) {
          assertThat(addressLine1).isEqualTo("Probation House, 31 High Street")
          assertThat(addressLine2).isEqualTo("City Centre")
          assertThat(postcode).isEqualTo("S1 3GG")
        }
        assertThat(response.emailAddresses).hasSize(1)
        assertThat(response.emailAddresses[0].address).isEqualTo("sheffield.probation@justice.gov.uk")
        assertThat(response.phoneNumbers).hasSize(1)
        assertThat(response.phoneNumbers[0].number).isEqualTo("0114 555 7777")
      }

      @Test
      fun `returns details for an approved premise`() {
        dsl.approvedPremise(
          approvedPremiseId = "SHEFAP",
          name = "Sheffield Approved Premises",
          description = "Sheffield City Centre Approved Premises",
          contact = "Gemma Smith",
          cjitCode = "123456789",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          localAuthorityCode = "00CG",
        ) {
          address(addressLine1 = "14 West Bar", addressLine2 = "City Centre", town = "Sheffield", county = "South Yorkshire", postcode = "S3 8PT", country = "England")
          email(emailAddress = "sheffield.approvedpremises@justice.gov.uk")
          phoneNumber(phoneNumber = "0114 555 8888")
        }

        val response: LegacyAgencyDto = webTestClient.get()
          .uri("/legacy/reconciliation/SHEFAP")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyType).isEqualTo(LegacyAgencyType.APPROVED_PREMISE)
        assertThat(response.name).isEqualTo("Sheffield Approved Premises")
        assertThat(response.description).isEqualTo("Sheffield City Centre Approved Premises")
        assertThat(response.contact).isEqualTo("Gemma Smith")
        assertThat(response.cjitCode).isEqualTo("123456789")
        assertThat(response.areaCode).isEqualTo("52")
        assertThat(response.regionCode).isEqualTo("YOHUM")
        assertThat(response.geographicalAreaCode).isEqualTo("WYORKS")
        assertThat(response.localAuthorityCode).isEqualTo("00CG")
        assertThat(response.addresses).hasSize(1)
        with(response.addresses[0]) {
          assertThat(addressLine1).isEqualTo("14 West Bar")
          assertThat(addressLine2).isEqualTo("City Centre")
          assertThat(postcode).isEqualTo("S3 8PT")
        }
        assertThat(response.emailAddresses).hasSize(1)
        assertThat(response.emailAddresses[0].address).isEqualTo("sheffield.approvedpremises@justice.gov.uk")
        assertThat(response.phoneNumbers).hasSize(1)
        assertThat(response.phoneNumbers[0].number).isEqualTo("0114 555 8888")
      }

      @Test
      fun `returns details for a police custody suite`() {
        dsl.policeCustodySuite(
          policeCustodySuiteId = "SHEFPS",
          name = "Sheffield Police Station",
          description = "Sheffield Central Police Station",
          cjitCode = "123456789",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          localAuthorityCode = "00CG",
        ) {
          address(addressLine1 = "101 Snig Hill", town = "Sheffield", county = "South Yorkshire", postcode = "S3 8LY", country = "England")
          email(emailAddress = "sheffield.police@example.com")
          phoneNumber(phoneNumber = "0114 220 2020")
        }

        val response: LegacyAgencyDto = webTestClient.get()
          .uri("/legacy/reconciliation/SHEFPS")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyType).isEqualTo(LegacyAgencyType.POLICE_CUSTODY_SUITE)
        assertThat(response.name).isEqualTo("Sheffield Police Station")
        assertThat(response.description).isEqualTo("Sheffield Central Police Station")
        assertThat(response.cjitCode).isEqualTo("123456789")
        assertThat(response.areaCode).isEqualTo("52")
        assertThat(response.regionCode).isEqualTo("YOHUM")
        assertThat(response.geographicalAreaCode).isEqualTo("WYORKS")
        assertThat(response.localAuthorityCode).isEqualTo("00CG")
        assertThat(response.addresses).hasSize(1)
        with(response.addresses[0]) {
          assertThat(addressLine1).isEqualTo("101 Snig Hill")
          assertThat(postcode).isEqualTo("S3 8LY")
        }
        assertThat(response.emailAddresses).hasSize(1)
        assertThat(response.emailAddresses[0].address).isEqualTo("sheffield.police@example.com")
        assertThat(response.phoneNumbers).hasSize(1)
        assertThat(response.phoneNumbers[0].number).isEqualTo("0114 220 2020")
      }

      @Test
      fun `returns details for a generic agency`() {
        dsl.agency(
          agencyId = "SFCRC",
          name = "Sheffield CRC",
          description = "Sheffield Community Rehabilitation Company",
          agencyType = AgencyType.PROBATION_CRC,
          cjitCode = "123456789",
          areaCode = "52",
          regionCode = "YOHUM",
          geographicalAreaCode = "WYORKS",
          payrollRegionCode = "HS",
          localAuthorityCode = "00CG",
        ) {
          address(addressLine1 = "1 Charter Row", town = "Sheffield", county = "South Yorkshire", postcode = "S1 3FZ", country = "England")
          email(emailAddress = "sheffield.crc@justice.gov.uk")
          phoneNumber(phoneNumber = "0114 555 1234")
        }

        val response: LegacyAgencyDto = webTestClient.get()
          .uri("/legacy/reconciliation/SFCRC")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        assertThat(response.agencyType).isEqualTo(LegacyAgencyType.PROBATION_CRC)
        assertThat(response.name).isEqualTo("Sheffield CRC")
        assertThat(response.description).isEqualTo("Sheffield Community Rehabilitation Company")
        assertThat(response.cjitCode).isEqualTo("123456789")
        assertThat(response.areaCode).isEqualTo("52")
        assertThat(response.regionCode).isEqualTo("YOHUM")
        assertThat(response.geographicalAreaCode).isEqualTo("WYORKS")
        assertThat(response.payrollRegionCode).isEqualTo("HS")
        assertThat(response.localAuthorityCode).isEqualTo("00CG")
        assertThat(response.addresses).hasSize(1)
        with(response.addresses[0]) {
          assertThat(addressLine1).isEqualTo("1 Charter Row")
          assertThat(postcode).isEqualTo("S1 3FZ")
        }
        assertThat(response.emailAddresses).hasSize(1)
        assertThat(response.emailAddresses[0].address).isEqualTo("sheffield.crc@justice.gov.uk")
        assertThat(response.phoneNumbers).hasSize(1)
        assertThat(response.phoneNumbers[0].number).isEqualTo("0114 555 1234")
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 404 when agency is not found`() {
        webTestClient.get()
          .uri("/legacy/reconciliation/NOTFND")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }
}
