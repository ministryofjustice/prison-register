package uk.gov.justice.digital.hmpps.prisonregister.resource

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.Category
import uk.gov.justice.digital.hmpps.prisonregister.model.Operator
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonType
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class PrisonDetailsResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var prisonRepository: PrisonRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @Autowired
  lateinit var entityManager: EntityManager

  @DisplayName("Get prison details by id")
  @Nested
  inner class GetById {
    lateinit var prison: Prison
    private var operators: Set<Operator> = emptySet()
    private var mainAddressId: Long = 0
    private var secondAddressId: Long = 0

    @BeforeEach
    fun setUp() {
      transactionHelper.runInTransaction {
        prison = dsl.prison(
          prisonId = "TSTPRI",
          name = "Test Prison",
          prisonNameInWelsh = "Test Prison Welsh name",
          active = false,
          male = true,
          female = true,
          contracted = true,
          lthse = true,
          inactiveDate = LocalDate.parse("2020-01-02"),
          cjitCode = "C00TP00",
          areaCode = "52",
          geographicalAreaCode = "WYORKS",
          regionCode = "YOHUM",
          payrollRegionCode = "HS",
          localAuthorityCode = "00CG",
        ) {
          address(
            addressLine1 = "Bawtry Road",
            addressLine2 = "Hatfield Woodhouse",
            town = "Doncaster",
            county = "South Yorkshire",
            postcode = "DN7 6BW",
            country = "England",
          )
          address(town = "Doncaster", postcode = "DN7 6EE", country = "England")
        }
        prison.prisonTypes.addAll(
          setOf(
            PrisonType(type = Type.HMP, prison = prison),
            PrisonType(type = Type.YOI, prison = prison),
          ),
        )
        prison.categories.addAll(setOf(Category.B, Category.C))
        operators = setOf(Operator(name = "Test PSP"), Operator(name = "Test G4S"))
        operators.forEach { entityManager.persist(it) }
        prison.prisonOperators = operators
        prison = prisonRepository.saveAndFlush(prison)
        mainAddressId = requireNotNull(prison.addresses.single { it.postcode == "DN7 6BW" }.id)
        secondAddressId = requireNotNull(prison.addresses.single { it.postcode == "DN7 6EE" }.id)
        val mainAddress = entityManager.find(Address::class.java, mainAddressId)
        mainAddress.addressLine1InWelsh = "Welsh address line 1"
        mainAddress.addressLine2InWelsh = "Welsh address line 2"
        mainAddress.townInWelsh = "Welsh town"
        mainAddress.countyInWelsh = "Welsh county"
        mainAddress.countryInWelsh = "Welsh country"
      }
    }

    @AfterEach
    fun tearDown() {
      transactionHelper.runInTransaction {
        if (::prison.isInitialized) {
          prisonRepository.deleteById(prison.prisonId)
          prisonRepository.flush()
        }
        operators.forEach { entityManager.remove(entityManager.find(Operator::class.java, it.id)) }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.get()
          .uri("/prison-maintenance/id/TSTPRI/details")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `requires correct role`() {
        webTestClient.get()
          .uri("/prison-maintenance/id/TSTPRI/details")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `allowed with correct role`() {
        webTestClient.get()
          .uri("/prison-maintenance/id/TSTPRI/details")
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 if not found`() {
        webTestClient.get()
          .uri("/prison-maintenance/id/ZZZZ/details")
          .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `400 if prison id is outside permitted length`() {
        listOf("AB", "ABCDEFG").forEach { prisonId ->
          webTestClient.get()
            .uri("/prison-maintenance/id/$prisonId/details")
            .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
            .exchange()
            .expectStatus().isBadRequest
        }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return core details and reference data`() {
        val prisonDto = getDetails()

        assertThat(prisonDto.prisonId).isEqualTo("TSTPRI")
        assertThat(prisonDto.prisonName).isEqualTo("Test Prison")
        assertThat(prisonDto.prisonNameInWelsh).isEqualTo("Test Prison Welsh name")
        assertThat(prisonDto.active).isFalse
        assertThat(prisonDto.male).isTrue
        assertThat(prisonDto.female).isTrue
        assertThat(prisonDto.contracted).isTrue
        assertThat(prisonDto.lthse).isTrue
        assertThat(prisonDto.inactiveDate).isEqualTo(LocalDate.parse("2020-01-02"))
        assertThat(prisonDto.cjitCode).isEqualTo("C00TP00")
        assertThat(prisonDto.area).isEqualTo(CodeDescription("52", "South Yorkshire"))
        assertThat(prisonDto.region).isEqualTo(CodeDescription("YOHUM", "Yorkshire & Humberside"))
        assertThat(prisonDto.geographicalArea).isEqualTo(CodeDescription("WYORKS", "West Yorkshire"))
        assertThat(prisonDto.localAuthority).isEqualTo(CodeDescription("00CG", "Sheffield City Council"))
        assertThat(prisonDto.payrollRegion).isEqualTo(CodeDescription("HS", "High Security"))
      }

      @Test
      fun `will return all addresses including Welsh fields`() {
        assertThat(getDetails().addresses).containsExactlyInAnyOrder(
          AddressDto(
            id = mainAddressId,
            addressLine1 = "Bawtry Road",
            addressLine2 = "Hatfield Woodhouse",
            town = "Doncaster",
            county = "South Yorkshire",
            postcode = "DN7 6BW",
            country = "England",
            addressLine1InWelsh = "Welsh address line 1",
            addressLine2InWelsh = "Welsh address line 2",
            townInWelsh = "Welsh town",
            countyInWelsh = "Welsh county",
            countryInWelsh = "Welsh country",
          ),
          AddressDto(
            id = secondAddressId,
            addressLine1 = null,
            addressLine2 = null,
            town = "Doncaster",
            county = null,
            postcode = "DN7 6EE",
            country = "England",
            addressLine1InWelsh = null,
            addressLine2InWelsh = null,
            townInWelsh = null,
            countyInWelsh = null,
            countryInWelsh = null,
          ),
        )
      }

      @Test
      fun `will return types categories and operators`() {
        val prisonDto = getDetails()

        assertThat(prisonDto.types).containsExactlyInAnyOrder(
          PrisonTypeDto(Type.HMP, Type.HMP.description),
          PrisonTypeDto(Type.YOI, Type.YOI.description),
        )
        assertThat(prisonDto.categories).containsExactlyInAnyOrder(Category.B, Category.C)
        assertThat(prisonDto.operators).containsExactlyInAnyOrder(PrisonOperatorDto("Test PSP"), PrisonOperatorDto("Test G4S"))
      }

      @Test
      fun `will return details without optional data`() {
        transactionHelper.runInTransaction {
          val storedPrison = prisonRepository.findById("TSTPRI").orElseThrow()
          storedPrison.active = true
          storedPrison.male = false
          storedPrison.female = false
          storedPrison.contracted = false
          storedPrison.lthse = false
          storedPrison.prisonNameInWelsh = null
          storedPrison.inactiveDate = null
          storedPrison.cjitCode = null
          storedPrison.area = null
          storedPrison.region = null
          storedPrison.geographicalArea = null
          storedPrison.localAuthority = null
          storedPrison.payrollRegion = null
          (storedPrison.addresses as MutableSet<Address>).clear()
          storedPrison.prisonTypes.clear()
          storedPrison.categories.clear()
          storedPrison.prisonOperators = emptySet()
        }

        val prisonDto = getDetails()

        assertThat(prisonDto.active).isTrue
        assertThat(prisonDto.male).isFalse
        assertThat(prisonDto.female).isFalse
        assertThat(prisonDto.contracted).isFalse
        assertThat(prisonDto.lthse).isFalse
        assertThat(prisonDto.prisonNameInWelsh).isNull()
        assertThat(prisonDto.inactiveDate).isNull()
        assertThat(prisonDto.cjitCode).isNull()
        assertThat(prisonDto.area).isNull()
        assertThat(prisonDto.region).isNull()
        assertThat(prisonDto.geographicalArea).isNull()
        assertThat(prisonDto.localAuthority).isNull()
        assertThat(prisonDto.payrollRegion).isNull()
        assertThat(prisonDto.addresses).isEmpty()
        assertThat(prisonDto.types).isEmpty()
        assertThat(prisonDto.categories).isEmpty()
        assertThat(prisonDto.operators).isEmpty()
      }

      private fun getDetails(): FullPrisonDto = webTestClient.get()
        .uri("/prison-maintenance/id/TSTPRI/details")
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBodyResponse()
    }
  }
}
