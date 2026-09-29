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
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremises
import uk.gov.justice.digital.hmpps.prisonregister.model.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Court
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.CourtTypeRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Hospital
import uk.gov.justice.digital.hmpps.prisonregister.model.HospitalRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgency
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuite
import uk.gov.justice.digital.hmpps.prisonregister.model.PoliceCustodySuiteRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOffice
import uk.gov.justice.digital.hmpps.prisonregister.model.ProbationOfficeRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription
import java.time.LocalDate

class PublicApiResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var courtRepository: CourtRepository

  @Autowired
  lateinit var hospitalRepository: HospitalRepository

  @Autowired
  lateinit var policeCustodySuiteRepository: PoliceCustodySuiteRepository

  @Autowired
  lateinit var probationOfficeRepository: ProbationOfficeRepository

  @Autowired
  lateinit var prisonRepository: PrisonRepository

  @Autowired
  lateinit var otherAgencyRepository: OtherAgencyRepository

  @Autowired
  lateinit var approvedPremisesRepository: ApprovedPremisesRepository

  @Autowired
  lateinit var areaRepository: AreaRepository

  @Autowired
  lateinit var regionRepository: RegionRepository

  @Autowired
  lateinit var courtTypeRepository: CourtTypeRepository

  @MockitoBean
  private lateinit var telemetryClient: TelemetryClient

  @DisplayName("Get all agencies")
  @Nested
  inner class GetAll {
    lateinit var court: Court
    lateinit var hospital: Hospital
    lateinit var policeCustodySuite: PoliceCustodySuite
    lateinit var probationOffice: ProbationOffice
    lateinit var prison: Prison
    lateinit var otherAgency: OtherAgency

    @BeforeEach
    fun setUp() {
      court = dsl.court(
        courtId = "DCOURT",
        name = "Doncaster Court",
        description = "Doncaster Central Court",
        active = true,
      ) {}

      hospital = dsl.hospital(
        hospitalId = "CHOSP",
        name = "Central Hospital",
        description = "Central Secure Hospital",
        active = true,
        highSecurity = true,
      ) {}

      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "BPOLIC",
        name = "Bawtry Police Custody Suite",
        description = "Bawtry Police Custody Suite",
        active = false,
      ) {}

      probationOffice = dsl.probationOffice(
        probationOfficeId = "APROB",
        name = "Aire Probation Office",
        description = "Aire Valley Probation Office",
        active = true,
      ) {}

      prison = dsl.prison(
        prisonId = "FPRIS",
        name = "Fictional Prison",
        description = "Fictional Prison Description",
        active = true,
      ) {}

      otherAgency = otherAgencyRepository.save(
        OtherAgency(
          agencyId = "EAGEN",
          name = "Example Agency",
          description = "Example Agency Description",
          active = true,
          accessibleAccess = null,
          otherAgencyType = OtherAgencyType.AIRPORT,
          inactiveDate = null,
          cjitCode = null,
          area = null,
          region = null,
          geographicalArea = null,
          payrollRegion = null,
          localAuthority = null,
        ),
      )
    }

    @AfterEach
    fun tearDown() {
      courtRepository.deleteById(court.courtId)
      hospitalRepository.deleteById(hospital.hospitalId)
      policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      probationOfficeRepository.deleteById(probationOffice.probationOfficeId)
      prisonRepository.deleteById(prison.prisonId)
      otherAgencyRepository.deleteById(otherAgency.agencyId)
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/api")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/api/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/api/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return all agencies, ordered by agency ID ascending`() {
        val agencies = webTestClient.get()
          .uri("/api/agencies")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
          .exchange()
          .expectStatus().isOk
          .expectBodyList(AgencySummaryDto::class.java)
          .returnResult()
          .responseBody!!

        assertThat(agencies).extracting("agencyId").contains("DCOURT", "CHOSP", "BPOLIC", "APROB", "FPRIS", "EAGEN")

        val ourAgencies = agencies.filter { it.agencyId in listOf("DCOURT", "CHOSP", "BPOLIC", "APROB", "FPRIS", "EAGEN") }
        assertThat(ourAgencies).isSortedAccordingTo(compareBy { it.agencyId })

        val courtDto = agencies.first { it.agencyId == "DCOURT" }
        assertThat(courtDto.description).isEqualTo("Doncaster Central Court")
        assertThat(courtDto.agencyType).isEqualTo(LegacyAgencyType.COURT)
        assertThat(courtDto.active).isTrue

        val hospitalDto = agencies.first { it.agencyId == "CHOSP" }
        assertThat(hospitalDto.description).isEqualTo("Central Secure Hospital")
        assertThat(hospitalDto.agencyType).isEqualTo(LegacyAgencyType.SECURE_HOSPITAL)
        assertThat(hospitalDto.active).isTrue

        val policeCustodySuiteDto = agencies.first { it.agencyId == "BPOLIC" }
        assertThat(policeCustodySuiteDto.description).isEqualTo("Bawtry Police Custody Suite")
        assertThat(policeCustodySuiteDto.agencyType).isEqualTo(LegacyAgencyType.POLICE_CUSTODY_SUITE)
        assertThat(policeCustodySuiteDto.active).isFalse

        val probationOfficeDto = agencies.first { it.agencyId == "APROB" }
        assertThat(probationOfficeDto.description).isEqualTo("Aire Valley Probation Office")
        assertThat(probationOfficeDto.agencyType).isEqualTo(LegacyAgencyType.PROBATION_OFFICE)
        assertThat(probationOfficeDto.active).isTrue

        val prisonDto = agencies.first { it.agencyId == "FPRIS" }
        assertThat(prisonDto.description).isEqualTo("Fictional Prison Description")
        assertThat(prisonDto.agencyType).isEqualTo(LegacyAgencyType.PRISON)
        assertThat(prisonDto.active).isTrue

        val otherAgencyDto = agencies.first { it.agencyId == "EAGEN" }
        assertThat(otherAgencyDto.description).isEqualTo("Example Agency Description")
        assertThat(otherAgencyDto.agencyType).isEqualTo(LegacyAgencyType.AIRPORT)
        assertThat(otherAgencyDto.active).isTrue
      }
    }
  }

  @DisplayName("Get agency by ID")
  @Nested
  inner class GetOne {
    lateinit var court: Court
    lateinit var hospital: Hospital
    lateinit var policeCustodySuite: PoliceCustodySuite
    lateinit var probationOffice: ProbationOffice
    lateinit var approvedPremises: ApprovedPremises
    lateinit var otherAgency: OtherAgency
    lateinit var prison: Prison

    @BeforeEach
    fun setUp() {
      court = dsl.court(
        courtId = "DCOURT",
        name = "Doncaster Court",
        description = "Doncaster Central Court",
        active = true,
        accessibleAccess = AccessibleAccess.ACCESSIBLE,
        inactiveDate = null,
        cjitCode = "CJIT001",
        courtTypeCode = "CC",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {
        address(addressLine1 = "1 High Street", town = "Doncaster", county = "South Yorkshire", postcode = "DN1 1AA", country = "England")
        email(emailAddress = "court@justice.gov.uk")
        phoneNumber(phoneNumber = "0114 555 1111")
      }

      hospital = dsl.hospital(
        hospitalId = "CHOSP",
        name = "Central Hospital",
        description = "Central Secure Hospital",
        active = true,
        highSecurity = true,
        inactiveDate = null,
        cjitCode = "CJIT002",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {
        address(addressLine1 = "2 Hospital Road", town = "Sheffield", county = "South Yorkshire", postcode = "S1 1AA", country = "England")
        phoneNumber(phoneNumber = "0114 555 2222")
      }

      policeCustodySuite = dsl.policeCustodySuite(
        policeCustodySuiteId = "BPOLIC",
        name = "Bawtry Police Custody Suite",
        description = "Bawtry Police Custody Suite",
        active = false,
        inactiveDate = LocalDate.of(2023, 12, 31),
        cjitCode = "CJIT003",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {
        address(addressLine1 = "3 Police Lane", town = "Bawtry", county = "South Yorkshire", postcode = "DN10 1AA", country = "England")
        email(emailAddress = "police@justice.gov.uk")
        phoneNumber(phoneNumber = "0114 555 3333")
      }

      probationOffice = dsl.probationOffice(
        probationOfficeId = "APROB",
        name = "Aire Probation Office",
        description = "Aire Valley Probation Office",
        contact = "Jane Doe",
        active = true,
        accessibleAccess = AccessibleAccess.WHEELCHAIR_ACCESS,
        inactiveDate = null,
        cjitCode = "CJIT004",
        areaCode = "52",
        subareaCode = "SHEFF",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {
        address(addressLine1 = "4 Aire Street", town = "Leeds", county = "West Yorkshire", postcode = "LS1 1AA", country = "England")
        email(emailAddress = "probation@justice.gov.uk")
        phoneNumber(phoneNumber = "0114 555 4444")
      }

      approvedPremises = dsl.approvedPremises(
        approvedPremisesId = "DAPPRE",
        name = "Doncaster Approved Premises",
        description = "Example Approved Premises Description",
        contact = "John Smith",
        active = true,
        accessibleAccess = AccessibleAccess.BY_ARRANGEMENT_ONLY,
        inactiveDate = null,
        cjitCode = "CJIT005",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {
        address(addressLine1 = "5 Approved Road", town = "Doncaster", county = "South Yorkshire", postcode = "DN2 2AA", country = "England")
        email(emailAddress = "ap@justice.gov.uk")
        phoneNumber(phoneNumber = "0114 555 5555")
      }

      otherAgency = dsl.agency(
        agencyId = "EAGEN",
        name = "Example Agency",
        description = "Example Agency Description",
        active = true,
        accessibleAccess = AccessibleAccess.NONE,
        otherAgencyType = OtherAgencyType.AIRPORT,
        inactiveDate = null,
        cjitCode = "CJIT006",
        areaCode = "52",
        regionCode = "YOHUM",
        geographicalAreaCode = "WYORKS",
        payrollRegionCode = "NEY",
        localAuthorityCode = "00CG",
      ) {
        address(addressLine1 = "6 Airport Way", town = "Doncaster", county = "South Yorkshire", postcode = "DN3 3AA", country = "England")
        email(emailAddress = "agency@justice.gov.uk")
        phoneNumber(phoneNumber = "0114 555 6666")
      }

      prison = dsl.prison(
        prisonId = "FPRIS",
        name = "Full Sutton Prison",
        description = "Full Sutton HM Prison",
        active = true,
      ) {}
      prison.addresses = setOf(
        Address(
          addressLine1 = "7 Prison Road",
          addressLine2 = null,
          town = "York",
          county = "North Yorkshire",
          postcode = "YO1 1AA",
          country = "England",
          prison = prison,
        ),
      )
      prisonRepository.save(prison)
    }

    @AfterEach
    fun tearDown() {
      courtRepository.deleteById(court.courtId)
      hospitalRepository.deleteById(hospital.hospitalId)
      policeCustodySuiteRepository.deleteById(policeCustodySuite.policeCustodySuiteId)
      probationOfficeRepository.deleteById(probationOffice.probationOfficeId)
      approvedPremisesRepository.deleteById(approvedPremises.approvedPremisesId)
      otherAgencyRepository.deleteById(otherAgency.agencyId)
      prisonRepository.deleteById(prison.prisonId)
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/api/agencies/DCOURT")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/api/agencies/DCOURT")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/api/agencies/DCOURT")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      private fun getAgency(agencyId: String) = webTestClient.get()
        .uri("/api/agencies/$agencyId")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
        .exchange()
        .expectStatus().isOk
        .expectBody(AgencyDetailsDto::class.java)
        .returnResult()
        .responseBody!!

      @Test
      fun `will return full details of a court`() {
        val dto = getAgency("DCOURT")

        val area = areaRepository.findByIdOrNull("52")!!
        val region = regionRepository.findByIdOrNull("YOHUM")!!
        val courtType = courtTypeRepository.findByIdOrNull("CC")!!

        assertThat(dto.agencyId).isEqualTo("DCOURT")
        assertThat(dto.description).isEqualTo("Doncaster Court")
        assertThat(dto.longDescription).isEqualTo("Doncaster Central Court")
        assertThat(dto.agencyType).isEqualTo(LegacyAgencyType.COURT)
        assertThat(dto.active).isTrue
        assertThat(dto.inactiveDate).isNull()
        assertThat(dto.area).isEqualTo(CodeDescription(area.code, area.description))
        assertThat(dto.region).isEqualTo(CodeDescription(region.code, region.description))
        assertThat(dto.courtType).isEqualTo(courtType.code)
        assertThat(dto.courtTypeDescription).isEqualTo(courtType.description)
        assertThat(dto.addresses).hasSize(1)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("1 High Street")
        assertThat(dto.addresses[0].town).isEqualTo("Doncaster")
        assertThat(dto.addresses[0].postcode).isEqualTo("DN1 1AA")
        assertThat(dto.emails).extracting("address").containsExactly("court@justice.gov.uk")
        assertThat(dto.phones).extracting("number").containsExactly("0114 555 1111")
      }

      @Test
      fun `will return full details of a hospital`() {
        val dto = getAgency("CHOSP")

        val area = areaRepository.findByIdOrNull("52")!!
        val region = regionRepository.findByIdOrNull("YOHUM")!!

        assertThat(dto.agencyId).isEqualTo("CHOSP")
        assertThat(dto.description).isEqualTo("Central Hospital")
        assertThat(dto.longDescription).isEqualTo("Central Secure Hospital")
        assertThat(dto.agencyType).isEqualTo(LegacyAgencyType.SECURE_HOSPITAL)
        assertThat(dto.active).isTrue
        assertThat(dto.inactiveDate).isNull()
        assertThat(dto.area).isEqualTo(CodeDescription(area.code, area.description))
        assertThat(dto.region).isEqualTo(CodeDescription(region.code, region.description))
        assertThat(dto.courtType).isNull()
        assertThat(dto.courtTypeDescription).isNull()
        assertThat(dto.addresses).hasSize(1)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("2 Hospital Road")
        assertThat(dto.addresses[0].town).isEqualTo("Sheffield")
        assertThat(dto.addresses[0].postcode).isEqualTo("S1 1AA")
        assertThat(dto.emails).isEmpty()
        assertThat(dto.phones).extracting("number").containsExactly("0114 555 2222")
      }

      @Test
      fun `will return full details of a police custody suite`() {
        val dto = getAgency("BPOLIC")

        val area = areaRepository.findByIdOrNull("52")!!
        val region = regionRepository.findByIdOrNull("YOHUM")!!

        assertThat(dto.agencyId).isEqualTo("BPOLIC")
        assertThat(dto.description).isEqualTo("Bawtry Police Custody Suite")
        assertThat(dto.longDescription).isEqualTo("Bawtry Police Custody Suite")
        assertThat(dto.agencyType).isEqualTo(LegacyAgencyType.POLICE_CUSTODY_SUITE)
        assertThat(dto.active).isFalse
        assertThat(dto.inactiveDate).isEqualTo(LocalDate.of(2023, 12, 31))
        assertThat(dto.area).isEqualTo(CodeDescription(area.code, area.description))
        assertThat(dto.region).isEqualTo(CodeDescription(region.code, region.description))
        assertThat(dto.courtType).isNull()
        assertThat(dto.courtTypeDescription).isNull()
        assertThat(dto.addresses).hasSize(1)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("3 Police Lane")
        assertThat(dto.addresses[0].town).isEqualTo("Bawtry")
        assertThat(dto.addresses[0].postcode).isEqualTo("DN10 1AA")
        assertThat(dto.emails).extracting("address").containsExactly("police@justice.gov.uk")
        assertThat(dto.phones).extracting("number").containsExactly("0114 555 3333")
      }

      @Test
      fun `will return full details of a probation office`() {
        val dto = getAgency("APROB")

        val area = areaRepository.findByIdOrNull("52")!!
        val region = regionRepository.findByIdOrNull("YOHUM")!!

        assertThat(dto.agencyId).isEqualTo("APROB")
        assertThat(dto.description).isEqualTo("Aire Probation Office")
        assertThat(dto.longDescription).isEqualTo("Aire Valley Probation Office")
        assertThat(dto.agencyType).isEqualTo(LegacyAgencyType.PROBATION_OFFICE)
        assertThat(dto.active).isTrue
        assertThat(dto.inactiveDate).isNull()
        assertThat(dto.area).isEqualTo(CodeDescription(area.code, area.description))
        assertThat(dto.region).isEqualTo(CodeDescription(region.code, region.description))
        assertThat(dto.courtType).isNull()
        assertThat(dto.courtTypeDescription).isNull()
        assertThat(dto.addresses).hasSize(1)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("4 Aire Street")
        assertThat(dto.addresses[0].town).isEqualTo("Leeds")
        assertThat(dto.addresses[0].postcode).isEqualTo("LS1 1AA")
        assertThat(dto.emails).extracting("address").containsExactly("probation@justice.gov.uk")
        assertThat(dto.phones).extracting("number").containsExactly("0114 555 4444")
      }

      @Test
      fun `will return full details of an approved premises`() {
        val dto = getAgency("DAPPRE")

        val area = areaRepository.findByIdOrNull("52")!!
        val region = regionRepository.findByIdOrNull("YOHUM")!!

        assertThat(dto.agencyId).isEqualTo("DAPPRE")
        assertThat(dto.description).isEqualTo("Doncaster Approved Premises")
        assertThat(dto.longDescription).isEqualTo("Example Approved Premises Description")
        assertThat(dto.agencyType).isEqualTo(LegacyAgencyType.APPROVED_PREMISES)
        assertThat(dto.active).isTrue
        assertThat(dto.inactiveDate).isNull()
        assertThat(dto.area).isEqualTo(CodeDescription(area.code, area.description))
        assertThat(dto.region).isEqualTo(CodeDescription(region.code, region.description))
        assertThat(dto.courtType).isNull()
        assertThat(dto.courtTypeDescription).isNull()
        assertThat(dto.addresses).hasSize(1)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("5 Approved Road")
        assertThat(dto.addresses[0].town).isEqualTo("Doncaster")
        assertThat(dto.addresses[0].postcode).isEqualTo("DN2 2AA")
        assertThat(dto.emails).extracting("address").containsExactly("ap@justice.gov.uk")
        assertThat(dto.phones).extracting("number").containsExactly("0114 555 5555")
      }

      @Test
      fun `will return full details of an other agency`() {
        val dto = getAgency("EAGEN")

        val area = areaRepository.findByIdOrNull("52")!!
        val region = regionRepository.findByIdOrNull("YOHUM")!!

        assertThat(dto.agencyId).isEqualTo("EAGEN")
        assertThat(dto.description).isEqualTo("Example Agency")
        assertThat(dto.longDescription).isEqualTo("Example Agency Description")
        assertThat(dto.agencyType).isEqualTo(LegacyAgencyType.AIRPORT)
        assertThat(dto.active).isTrue
        assertThat(dto.inactiveDate).isNull()
        assertThat(dto.area).isEqualTo(CodeDescription(area.code, area.description))
        assertThat(dto.region).isEqualTo(CodeDescription(region.code, region.description))
        assertThat(dto.courtType).isNull()
        assertThat(dto.courtTypeDescription).isNull()
        assertThat(dto.addresses).hasSize(1)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("6 Airport Way")
        assertThat(dto.addresses[0].town).isEqualTo("Doncaster")
        assertThat(dto.addresses[0].postcode).isEqualTo("DN3 3AA")
        assertThat(dto.emails).extracting("address").containsExactly("agency@justice.gov.uk")
        assertThat(dto.phones).extracting("number").containsExactly("0114 555 6666")
      }

      @Test
      fun `will return full details of a prison`() {
        val dto = getAgency("FPRIS")

        assertThat(dto.agencyId).isEqualTo("FPRIS")
        assertThat(dto.description).isEqualTo("Full Sutton Prison")
        assertThat(dto.longDescription).isEqualTo("Full Sutton HM Prison")
        assertThat(dto.agencyType).isEqualTo(LegacyAgencyType.PRISON)
        assertThat(dto.active).isTrue
        assertThat(dto.inactiveDate).isNull()
        assertThat(dto.area).isNull()
        assertThat(dto.region).isNull()
        assertThat(dto.courtType).isNull()
        assertThat(dto.courtTypeDescription).isNull()
        assertThat(dto.addresses).hasSize(1)
        assertThat(dto.addresses[0].addressLine1).isEqualTo("7 Prison Road")
        assertThat(dto.addresses[0].town).isEqualTo("York")
        assertThat(dto.addresses[0].postcode).isEqualTo("YO1 1AA")
        assertThat(dto.emails).isEmpty()
        assertThat(dto.phones).isEmpty()
      }

      @Test
      fun `will return 404 when agency does not exist`() {
        webTestClient.get()
          .uri("/api/agencies/NONE")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @DisplayName("Get agencies by type")
  @Nested
  inner class GetByType {
    lateinit var activeCourt: Court
    lateinit var inactiveCourt: Court
    lateinit var activeOtherAgency: OtherAgency
    lateinit var inactiveOtherAgency: OtherAgency

    @BeforeEach
    fun setUp() {
      activeCourt = dsl.court(
        courtId = "ACOURT",
        name = "Active Court",
        description = "Active Court Description",
        active = true,
      ) {}

      inactiveCourt = dsl.court(
        courtId = "ICOURT",
        name = "Inactive Court",
        description = "Inactive Court Description",
        active = false,
      ) {}

      activeOtherAgency = otherAgencyRepository.save(
        OtherAgency(
          agencyId = "AAGEN",
          name = "Active Airport",
          description = "Active Airport Description",
          active = true,
          accessibleAccess = null,
          otherAgencyType = OtherAgencyType.AIRPORT,
          inactiveDate = null,
          cjitCode = null,
          area = null,
          region = null,
          geographicalArea = null,
          payrollRegion = null,
          localAuthority = null,
        ),
      )

      inactiveOtherAgency = otherAgencyRepository.save(
        OtherAgency(
          agencyId = "IAGEN",
          name = "Inactive Airport",
          description = "Inactive Airport Description",
          active = false,
          accessibleAccess = null,
          otherAgencyType = OtherAgencyType.AIRPORT,
          inactiveDate = null,
          cjitCode = null,
          area = null,
          region = null,
          geographicalArea = null,
          payrollRegion = null,
          localAuthority = null,
        ),
      )
    }

    @AfterEach
    fun tearDown() {
      courtRepository.deleteById(activeCourt.courtId)
      courtRepository.deleteById(inactiveCourt.courtId)
      otherAgencyRepository.deleteById(activeOtherAgency.agencyId)
      otherAgencyRepository.deleteById(inactiveOtherAgency.agencyId)
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/api/agencies/type/COURT")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/api/agencies/type/COURT")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/api/agencies/type/COURT")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      private fun getAgenciesByType(type: String, activeOnly: Boolean? = null) = webTestClient.get()
        .uri { builder ->
          builder.path("/api/agencies/type/$type")
            .apply { activeOnly?.let { queryParam("activeOnly", it) } }
            .build()
        }
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
        .exchange()
        .expectStatus().isOk
        .expectBodyList(AgencyDetailsDto::class.java)
        .returnResult()
        .responseBody!!

      @Test
      fun `will only return active courts by default`() {
        val dtos = getAgenciesByType("COURT")

        assertThat(dtos).extracting("agencyId").contains("ACOURT").doesNotContain("ICOURT")
        val dto = dtos.first { it.agencyId == "ACOURT" }
        assertThat(dto.description).isEqualTo("Active Court")
        assertThat(dto.agencyType).isEqualTo(LegacyAgencyType.COURT)
        assertThat(dto.active).isTrue
      }

      @Test
      fun `will return all courts when activeOnly is false`() {
        val dtos = getAgenciesByType("COURT", activeOnly = false)

        assertThat(dtos).extracting("agencyId").contains("ACOURT", "ICOURT")
      }

      @Test
      fun `will only return active other agencies of the given type by default`() {
        val dtos = getAgenciesByType("AIRPORT")

        assertThat(dtos).extracting("agencyId").contains("AAGEN").doesNotContain("IAGEN")
        val dto = dtos.first { it.agencyId == "AAGEN" }
        assertThat(dto.description).isEqualTo("Active Airport")
        assertThat(dto.agencyType).isEqualTo(LegacyAgencyType.AIRPORT)
      }

      @Test
      fun `will return all other agencies of the given type when activeOnly is false`() {
        val dtos = getAgenciesByType("AIRPORT", activeOnly = false)

        assertThat(dtos).extracting("agencyId").contains("AAGEN", "IAGEN")
      }

      @Test
      fun `will return 400 for an unknown agency type`() {
        webTestClient.get()
          .uri("/api/agencies/type/NOT_A_TYPE")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }
  }

  @DisplayName("Get agencies by legacy type")
  @Nested
  inner class GetByLegacyType {
    lateinit var activeCourt: Court
    lateinit var inactiveCourt: Court
    lateinit var activeOtherAgency: OtherAgency
    lateinit var inactiveOtherAgency: OtherAgency
    lateinit var prison: Prison

    @BeforeEach
    fun setUp() {
      activeCourt = dsl.court(
        courtId = "ACOURT",
        name = "Active Court",
        description = "Active Court Description",
        active = true,
      ) {}

      inactiveCourt = dsl.court(
        courtId = "ICOURT",
        name = "Inactive Court",
        description = "Inactive Court Description",
        active = false,
      ) {}

      activeOtherAgency = otherAgencyRepository.save(
        OtherAgency(
          agencyId = "AAGEN",
          name = "Active Airport",
          description = "Active Airport Description",
          active = true,
          accessibleAccess = null,
          otherAgencyType = OtherAgencyType.AIRPORT,
          inactiveDate = null,
          cjitCode = null,
          area = null,
          region = null,
          geographicalArea = null,
          payrollRegion = null,
          localAuthority = null,
        ),
      )

      inactiveOtherAgency = otherAgencyRepository.save(
        OtherAgency(
          agencyId = "IAGEN",
          name = "Inactive Airport",
          description = "Inactive Airport Description",
          active = false,
          accessibleAccess = null,
          otherAgencyType = OtherAgencyType.AIRPORT,
          inactiveDate = null,
          cjitCode = null,
          area = null,
          region = null,
          geographicalArea = null,
          payrollRegion = null,
          localAuthority = null,
        ),
      )

      prison = dsl.prison(
        prisonId = "LPRIS",
        name = "Legacy Prison",
        description = "Legacy Prison Description",
        active = true,
      ) {}
    }

    @AfterEach
    fun tearDown() {
      courtRepository.deleteById(activeCourt.courtId)
      courtRepository.deleteById(inactiveCourt.courtId)
      otherAgencyRepository.deleteById(activeOtherAgency.agencyId)
      otherAgencyRepository.deleteById(inactiveOtherAgency.agencyId)
      prisonRepository.deleteById(prison.prisonId)
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/api/agencies/legacy-type/CRT")
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/api/agencies/legacy-type/CRT")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/api/agencies/legacy-type/CRT")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      private fun getAgenciesByLegacyType(type: String, activeOnly: Boolean? = null) = webTestClient.get()
        .uri { builder ->
          builder.path("/api/agencies/legacy-type/$type")
            .apply { activeOnly?.let { queryParam("activeOnly", it) } }
            .build()
        }
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
        .exchange()
        .expectStatus().isOk
        .expectBodyList(AgencyDetailsDto::class.java)
        .returnResult()
        .responseBody!!

      @Test
      fun `CRT maps to courts and only returns active ones by default`() {
        val dtos = getAgenciesByLegacyType("CRT")

        assertThat(dtos).extracting("agencyId").contains("ACOURT").doesNotContain("ICOURT")
        val dto = dtos.first { it.agencyId == "ACOURT" }
        assertThat(dto.agencyType).isEqualTo(LegacyAgencyType.COURT)
      }

      @Test
      fun `CRT with activeOnly false returns all courts`() {
        val dtos = getAgenciesByLegacyType("CRT", activeOnly = false)

        assertThat(dtos).extracting("agencyId").contains("ACOURT", "ICOURT")
      }

      @Test
      fun `AIRPORT maps to other agencies of type AIRPORT`() {
        val dtos = getAgenciesByLegacyType("AIRPORT")

        assertThat(dtos).extracting("agencyId").contains("AAGEN").doesNotContain("IAGEN")
        val dto = dtos.first { it.agencyId == "AAGEN" }
        assertThat(dto.agencyType).isEqualTo(LegacyAgencyType.AIRPORT)
      }

      @Test
      fun `INST maps to prisons`() {
        val dtos = getAgenciesByLegacyType("INST")

        assertThat(dtos).extracting("agencyId").contains("LPRIS")
        val dto = dtos.first { it.agencyId == "LPRIS" }
        assertThat(dto.agencyType).isEqualTo(LegacyAgencyType.PRISON)
      }

      @Test
      fun `POLSTN and POLICE both map to police custody suite`() {
        assertThat(getAgenciesByLegacyType("POLICE")).isEmpty()
        assertThat(getAgenciesByLegacyType("POLSTN")).isEmpty()
      }

      @Test
      fun `will return 400 for an unrecognised legacy type`() {
        webTestClient.get()
          .uri("/api/agencies/legacy-type/NOT_A_LEGACY_TYPE")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__R")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }
  }
}
