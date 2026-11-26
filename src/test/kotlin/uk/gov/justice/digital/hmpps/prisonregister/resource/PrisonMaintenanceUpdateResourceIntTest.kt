package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prisonregister.dsl.Root
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.model.Category
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.OperatorRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.SnsService
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper
import java.time.LocalDate

class PrisonMaintenanceUpdateResourceIntTest : IntegrationTestBase() {

  @MockitoBean
  private lateinit var auditService: AuditService

  @MockitoBean
  private lateinit var telemetryClient: TelemetryClient

  @MockitoSpyBean
  private lateinit var snsService: SnsService

  @Autowired
  lateinit var dsl: Root

  @Autowired
  lateinit var prisonRepository: PrisonRepository

  @Autowired
  lateinit var operatorRepository: OperatorRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @Nested
  inner class UpdatePrison {
    @Nested
    inner class Security {
      @Test
      fun `requires a valid authentication token`() {
        webTestClient.put()
          .uri("/prison-maintenance/id/MDI")
          .accept(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(UpdatePrisonDto("Updated Prison", active = false, male = true)))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `correct permission is needed to update prison data`() {
        webTestClient.put()
          .uri("/prison-maintenance/id/MDI")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_DUMMY"), scopes = listOf("write")))
          .body(BodyInserters.fromValue(UpdatePrisonDto("Updated Prison", active = false, male = true)))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class HappyPath {
      lateinit var prison: Prison

      @AfterEach
      fun tearDown() {
        prisonRepository.deleteById("TSTPRI")
      }

      @BeforeEach
      fun setUp() {
        transactionHelper.runInTransaction {
          prison = dsl.prison(
            prisonId = "TSTPRI",
            name = "Test Prison",
            description = "Test Prison description",
            prisonNameInWelsh = "Test Prison Welsh name",
            active = false,
            male = false,
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
            ).id

            category(Category.B)
            category(Category.C)

            operator("Test PSP")

            type(Type.HMP)
            type(Type.YOI)

            department(departmentType = DepartmentType.OFFICIAL_VISIT, emailAddress = "officialvisit@justice.gov.uk", phoneNumber = "0113 200 2000")
          }
        }
      }

      @Test
      fun `will leave operator unchanged when not provided`() {
        transactionHelper.runInTransaction {
          val operators = prisonRepository.findByIdOrNull(prison.prisonId)!!.prisonOperators
          assertThat(operators).hasSize(1)
          assertThat(operators.firstOrNull()!!.name).isEqualTo("Test PSP")
        }
        webTestClient.put()
          .uri("/prison-maintenance/id/TSTPRI")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write")))
          .bodyValue(
            UpdatePrisonDto(
              prisonName = prison.name,
              prisonNameInWelsh = prison.prisonNameInWelsh,
              prisonDescription = prison.description,
              active = prison.active,
              male = prison.male,
              female = prison.female,
              contracted = prison.contracted,
              lthse = prison.lthse,
              prisonTypes = setOf(Type.HMP, Type.YOI),
              categories = prison.categories,
              operator = null,
            ),
          )
          .exchange()
          .expectStatus().isOk

        transactionHelper.runInTransaction {
          val operators = prisonRepository.findByIdOrNull(prison.prisonId)!!.prisonOperators
          assertThat(operators).hasSize(1)
          assertThat(operators.firstOrNull()!!.name).isEqualTo("Test PSP")
        }
      }

      @Test
      fun `will set operator unchanged when provided`() {
        transactionHelper.runInTransaction {
          val operators = prisonRepository.findByIdOrNull(prison.prisonId)!!.prisonOperators
          assertThat(operators).hasSize(1)
          assertThat(operators.firstOrNull()!!.name).isEqualTo("Test PSP")
        }
        webTestClient.put()
          .uri("/prison-maintenance/id/TSTPRI")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write")))
          .bodyValue(
            UpdatePrisonDto(
              prisonName = prison.name,
              prisonNameInWelsh = prison.prisonNameInWelsh,
              prisonDescription = prison.description,
              active = prison.active,
              male = prison.male,
              female = prison.female,
              contracted = prison.contracted,
              lthse = prison.lthse,
              prisonTypes = setOf(Type.HMP, Type.YOI),
              categories = prison.categories,
              operator = operatorRepository.findByName("Serco")?.let { PrisonOperator(it.id, it.name) },
            ),
          )
          .exchange()
          .expectStatus().isOk

        transactionHelper.runInTransaction {
          val operators = prisonRepository.findByIdOrNull(prison.prisonId)!!.prisonOperators
          assertThat(operators).hasSize(1)
          assertThat(operators.firstOrNull()!!.name).isEqualTo("Serco")
        }
      }

      @Test
      fun `will update core details but leave everything else unchanged`() {
        transactionHelper.runInTransaction {
          val operators = prisonRepository.findByIdOrNull(prison.prisonId)!!.prisonOperators
          assertThat(operators).hasSize(1)
          assertThat(operators.firstOrNull()!!.name).isEqualTo("Test PSP")
        }
        webTestClient.put()
          .uri("/prison-maintenance/id/TSTPRI")
          .accept(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write")))
          .bodyValue(
            UpdatePrisonDto(
              prisonName = "Changed Prison Name",
              prisonNameInWelsh = "Changed Prison Name Welsh",
              prisonDescription = "Changed Prison Description",
              active = false,
              male = true,
              female = false,
              contracted = false,
              lthse = false,
              prisonTypes = setOf(Type.YCS),
              categories = setOf(Category.A),
              operator = operatorRepository.findByName("PSP")?.let { PrisonOperator(it.id, it.name) },
            ),
          )
          .exchange()
          .expectStatus().isOk

        transactionHelper.runInTransaction {
          val amendedPrison = prisonRepository.findByIdOrNull(prison.prisonId)!!
          with(amendedPrison) {
            assertThat(name).isEqualTo("Changed Prison Name")
            assertThat(prisonNameInWelsh).isEqualTo("Changed Prison Name Welsh")
            assertThat(description).isEqualTo("Changed Prison Description")
            assertThat(active).isFalse
            assertThat(male).isTrue
            assertThat(female).isFalse
            assertThat(contracted).isFalse
            assertThat(lthse).isFalse
            assertThat(prisonTypes).hasSize(1)
            assertThat(prisonTypes.firstOrNull()!!.type).isEqualTo(Type.YCS)
            assertThat(categories).hasSize(1)
            assertThat(categories.firstOrNull()).isEqualTo(Category.A)
            assertThat(prisonOperators).hasSize(1)
            assertThat(prisonOperators.firstOrNull()!!.name).isEqualTo("PSP")

            // remain unchanged
            assertThat(addresses).hasSize(1)
            assertThat(departments).hasSize(1)
            assertThat(cjitCode).isEqualTo("C00TP00")
            assertThat(area?.code).isEqualTo("52")
            assertThat(geographicalArea?.code).isEqualTo("WYORKS")
            assertThat(region?.code).isEqualTo("YOHUM")
            assertThat(payrollRegion?.code).isEqualTo("HS")
            assertThat(localAuthority?.code).isEqualTo("00CG")

            // this needs adding to this endpoint when deactivate is re-worked
            assertThat(inactiveDate).isEqualTo(LocalDate.parse("2020-01-02"))
          }
        }
      }
    }
  }
}
