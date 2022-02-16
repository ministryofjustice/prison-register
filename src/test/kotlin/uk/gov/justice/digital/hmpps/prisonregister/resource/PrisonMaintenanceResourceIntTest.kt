package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.model.EventType
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.SnsService
import uk.gov.justice.digital.hmpps.prisonregister.model.UpdatePrisonDto
import java.util.Optional

class PrisonMaintenanceResourceIntTest : IntegrationTest() {

  @MockBean
  private lateinit var prisonRepository: PrisonRepository

  @MockBean
  private lateinit var auditService: AuditService

  @MockBean
  private lateinit var snsService: SnsService

  @MockBean
  private lateinit var telemetryClient: TelemetryClient

  @Nested
  inner class UpdatePrisons {
    @Test
    fun `requires a valid authentication token`() {
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI")
        .accept(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(UpdatePrisonDto("Updated Prison", false)))
        .exchange()
        .expectStatus().isUnauthorized
    }
    @Test
    fun `correct permission is needed to update prison data`() {
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY"), scopes = listOf("write")))
        .body(BodyInserters.fromValue(UpdatePrisonDto("Updated Prison", false)))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `correct scope is needed to update prison data`() {
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("read")))
        .body(BodyInserters.fromValue(UpdatePrisonDto("Updated Prison", false)))
        .exchange().expectStatus().isForbidden
    }

    @Test
    fun `update a prison with bad data`() {
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "prisonName" to "A",
              "active" to "true"
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
      verifyNoInteractions(auditService)
      verifyNoInteractions(snsService)
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `update a prison`() {
      whenever(prisonRepository.findById("MDI")).thenReturn(
        Optional.of(Prison("MDI", "A Prison 1", true))
      )
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_MAINTAIN_REF_DATA"),
            scopes = listOf("write"),
            user = "bobby.beans"
          )
        )
        .body(BodyInserters.fromValue(UpdatePrisonDto("Updated Prison", false)))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("updated_prison".loadJson())

      verify(auditService).sendAuditEvent("PRISON_REGISTER_UPDATE", Pair("MDI", UpdatePrisonDto("Updated Prison", false)))
      verify(snsService).sendEvent(EventType.PRISON_REGISTER_UPDATE, "MDI")
      verify(telemetryClient).trackEvent(eq("prison-register-update"), any(), isNull())
    }
  }

  private fun String.loadJson(): String =
    PrisonMaintenanceResourceIntTest::class.java.getResource("$this.json")?.readText()
      ?: throw AssertionError("file $this.json not found")
}
