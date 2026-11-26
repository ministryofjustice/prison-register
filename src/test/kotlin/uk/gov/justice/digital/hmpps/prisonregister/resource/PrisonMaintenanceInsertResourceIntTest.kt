package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.model.OperatorRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.utilities.TransactionHelper

class PrisonMaintenanceInsertResourceIntTest : IntegrationTestBase() {

  @MockitoBean
  private lateinit var auditService: AuditService

  @MockitoBean
  private lateinit var telemetryClient: TelemetryClient

  @Autowired
  lateinit var prisonRepository: PrisonRepository

  @Autowired
  lateinit var operatorRepository: OperatorRepository

  @Autowired
  lateinit var transactionHelper: TransactionHelper

  @AfterEach
  fun tearDown() {
    prisonRepository.findByIdOrNull("TSTNEW")?.let { prisonRepository.delete(it) }
  }

  private fun insertPrison(operator: PrisonOperator?) = webTestClient.post()
    .uri("/prison-maintenance")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write")))
    .bodyValue(InsertPrisonDto(prisonId = "TSTNEW", prisonName = "Test New Prison", contracted = operator?.name != "PSP", operator = operator))
    .exchange()

  private fun operatorNamed(name: String) = operatorRepository.findByName(name)!!.let { PrisonOperator(it.id, it.name) }

  private fun savedOperatorNames(): List<String> = transactionHelper.runInTransaction {
    prisonRepository.findByIdOrNull("TSTNEW")!!.prisonOperators.map { it.name }
  }

  @Test
  fun `saves the provider of a contracted prison`() {
    insertPrison(operatorNamed("Serco"))
      .expectStatus().isCreated
      .expectBody().jsonPath("$.currentOperator.name").isEqualTo("Serco")

    assertThat(savedOperatorNames()).containsExactly("Serco")
  }

  @Test
  fun `saves PSP for a public sector prison`() {
    insertPrison(operatorNamed("PSP")).expectStatus().isCreated

    assertThat(savedOperatorNames()).containsExactly("PSP")
  }

  @Test
  fun `saves no operator when none is given, as before`() {
    insertPrison(operator = null).expectStatus().isCreated

    assertThat(savedOperatorNames()).isEmpty()
  }

  @Test
  fun `rejects an unknown operator and does not save the prison`() {
    insertPrison(PrisonOperator(id = 999_999, name = "Unknown"))
      .expectStatus().isBadRequest
      .expectBody().jsonPath("$.userMessage").isEqualTo("999999 operator id not found for prison TSTNEW")

    assertThat(prisonRepository.findByIdOrNull("TSTNEW")).isNull()
  }
}
