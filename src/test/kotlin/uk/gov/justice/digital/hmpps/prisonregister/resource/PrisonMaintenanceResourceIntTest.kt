package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.HMPPSDomainEvent
import java.util.Optional

class PrisonMaintenanceResourceIntTest(@Autowired private val objectMapper: ObjectMapper) : IntegrationTest() {

  @MockBean
  private lateinit var prisonRepository: PrisonRepository

  @MockBean
  private lateinit var auditService: AuditService

  @MockBean
  private lateinit var telemetryClient: TelemetryClient

  @Nested
  inner class UpdatePrison {
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
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `update a prison`() {
      whenever(prisonRepository.findById("MDI")).thenReturn(
        Optional.of(Prison("MDI", "A Prison 1", active = true))
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
        .body(BodyInserters.fromValue(UpdatePrisonDto("Updated Prison", false, setOf(Type.YOI))))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("updated_prison".loadJson())

      verify(auditService).sendAuditEvent(eq("PRISON_REGISTER_UPDATE"), eq(Pair("MDI", UpdatePrisonDto("Updated Prison", false, setOf(Type.YOI)))), any())
      await untilCallTo { testQueueEventMessageCount() } matches { it == 1 }

      val requestJson = testSqsClient.receiveMessage(testQueueUrl).messages[0].body
      val (message, messageId, messageAttributes) = objectMapper.readValue(requestJson, HMPPSMessage::class.java)
      assertThat(messageAttributes.eventType.Value).isEqualTo("register.prison.amended")

      val (eventType, additionalInformation) = objectMapper.readValue(message, HMPPSDomainEvent::class.java)
      assertThat(eventType).isEqualTo("register.prison.amended")
      assertThat(additionalInformation.prisonId).isEqualTo("MDI")
      assertThat(message.contains("A prison has been updated"))
      verify(telemetryClient).trackEvent(eq("prison-register-update"), any(), isNull())
    }
  }

  @Nested
  inner class InsertPrison {
    @Test
    fun `requires a valid authentication token`() {
      webTestClient.post()
        .uri("/prison-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(InsertPrisonDto("AAI", "Created Prison", false)))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `correct permission is needed to insert prison data`() {
      webTestClient.post()
        .uri("/prison-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY"), scopes = listOf("write")))
        .body(BodyInserters.fromValue(InsertPrisonDto("AAI", "Created Prison", false)))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `correct scope is needed to insert prison data`() {
      webTestClient.post()
        .uri("/prison-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("read")))
        .body(BodyInserters.fromValue(InsertPrisonDto("AAI", "Created Prison", false)))
        .exchange().expectStatus().isForbidden
    }

    @Test
    fun `insert a prison with bad data`() {
      webTestClient.post()
        .uri("/prison-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "prisonId" to "MDA",
              "prisonName" to "A",
              "active" to "true"
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
      verifyNoInteractions(auditService)
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `insert a prison`() {
      val prison = Prison("MDI", "Inserted Prison", active = false)
      whenever(prisonRepository.findById("MDI")).thenReturn(Optional.empty(), Optional.of(prison))
      whenever(prisonRepository.save(any())).thenReturn(prison)

      webTestClient.post()
        .uri("/prison-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_MAINTAIN_REF_DATA"),
            scopes = listOf("write"),
            user = "bobby.beans"
          )
        )
        .body(BodyInserters.fromValue(InsertPrisonDto("MDI", "Inserted Prison", false)))
        .exchange()
        .expectStatus().isCreated
        .expectBody().json("inserted_prison".loadJson())

      verify(auditService).sendAuditEvent(
        eq("PRISON_REGISTER_INSERT"),
        eq(InsertPrisonDto("MDI", "Inserted Prison", false)), any()
      )
      await untilCallTo { testQueueEventMessageCount() } matches { it == 1 }

      val requestJson = testSqsClient.receiveMessage(testQueueUrl).messages[0].body
      val (message, messageId, messageAttributes) = objectMapper.readValue(requestJson, HMPPSMessage::class.java)
      assertThat(messageAttributes.eventType.Value).isEqualTo("register.prison.inserted")

      val (eventType, additionalInformation) = objectMapper.readValue(message, HMPPSDomainEvent::class.java)
      assertThat(eventType).isEqualTo("register.prison.inserted")
      assertThat(additionalInformation.prisonId).isEqualTo("MDI")
      assertThat(message.contains("A prison has been inserted"))
      verify(telemetryClient).trackEvent(eq("prison-register-insert"), any(), isNull())
    }
  }

  private fun String.loadJson(): String =
    PrisonMaintenanceResourceIntTest::class.java.getResource("$this.json")?.readText()
      ?: throw AssertionError("file $this.json not found")
}
