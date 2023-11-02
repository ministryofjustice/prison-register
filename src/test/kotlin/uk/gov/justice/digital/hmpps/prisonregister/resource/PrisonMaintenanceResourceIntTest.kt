package uk.gov.justice.digital.hmpps.prisonregister.resource

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
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonType
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.HMPPSDomainEvent
import java.util.Optional

class PrisonMaintenanceResourceIntTest() : IntegrationTest() {

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
        .body(BodyInserters.fromValue(UpdatePrisonDto("Updated Prison", false, male = true)))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `correct permission is needed to update prison data`() {
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY"), scopes = listOf("write")))
        .body(BodyInserters.fromValue(UpdatePrisonDto("Updated Prison", false, female = true)))
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
              "active" to "true",
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
      verifyNoInteractions(auditService)
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `update a prison with maintain ref data role`() {
      whenever(prisonRepository.findById("MDI")).thenReturn(
        Optional.of(Prison("MDI", "A Prison 1", active = true)),
      )
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_MAINTAIN_REF_DATA"),
            scopes = listOf("write"),
            user = "bobby.beans",
          ),
        )
        .body(BodyInserters.fromValue(UpdatePrisonDto("Updated Prison", false, male = true, female = true, contracted = true, setOf(Type.YOI))))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("updated_prison".loadJson())

      verify(auditService).sendAuditEvent(
        eq("PRISON_REGISTER_UPDATE"),
        eq(
          Pair(
            "MDI",
            UpdatePrisonDto("Updated Prison", false, male = true, female = true, contracted = true, setOf(Type.YOI)),
          ),
        ),
        any(),
      )
      await untilCallTo { testQueueEventMessageCount() } matches { it == 1 }

      val requestJson = testSqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testQueueUrl).build()).get().messages()[0].body()
      val (message, messageId, messageAttributes) = objectMapper.readValue(requestJson, HMPPSMessage::class.java)
      assertThat(messageAttributes.eventType.Value).isEqualTo("register.prison.amended")

      val (eventType, additionalInformation) = objectMapper.readValue(message, HMPPSDomainEvent::class.java)
      assertThat(eventType).isEqualTo("register.prison.amended")
      assertThat(additionalInformation.prisonId).isEqualTo("MDI")
      assertThat(message.contains("A prison has been updated"))
      verify(telemetryClient).trackEvent(eq("prison-register-update"), any(), isNull())
    }

    @Test
    fun `update a prison with maintain prison data role`() {
      whenever(prisonRepository.findById("MDI")).thenReturn(
        Optional.of(Prison("MDI", "A Prison 1", active = true)),
      )
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_MAINTAIN_PRISON_DATA"),
            scopes = listOf("write"),
            user = "bobby.beans",
          ),
        )
        .body(BodyInserters.fromValue(UpdatePrisonDto("Updated Prison", false, male = true, female = true, contracted = true, setOf(Type.YOI))))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("updated_prison".loadJson())

      verify(auditService).sendAuditEvent(
        eq("PRISON_REGISTER_UPDATE"),
        eq(
          Pair(
            "MDI",
            UpdatePrisonDto("Updated Prison", false, male = true, female = true, contracted = true, setOf(Type.YOI)),
          ),
        ),
        any(),
      )
      await untilCallTo { testQueueEventMessageCount() } matches { it == 1 }

      val requestJson = testSqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testQueueUrl).build()).get().messages()[0].body()
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
        .body(BodyInserters.fromValue(InsertPrisonDto("AAI", "Created Prison", false, contracted = false)))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `correct permission is needed to insert prison data`() {
      webTestClient.post()
        .uri("/prison-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY"), scopes = listOf("write")))
        .body(BodyInserters.fromValue(InsertPrisonDto("AAI", "Created Prison", false, contracted = false)))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `correct scope is needed to insert prison data`() {
      webTestClient.post()
        .uri("/prison-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("read")))
        .body(BodyInserters.fromValue(InsertPrisonDto("AAI", "Created Prison", false, contracted = false)))
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
              "active" to "true",
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
      verifyNoInteractions(auditService)
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `insert a prison with minimal data`() {
      val prison = Prison("MDI", "Inserted Prison", active = true)
      whenever(prisonRepository.findById("MDI")).thenReturn(Optional.empty(), Optional.of(prison))
      whenever(prisonRepository.save(any())).thenReturn(prison)
      val insertDto = InsertPrisonDto("MDI", "Inserted Prison", contracted = false)

      webTestClient.post()
        .uri("/prison-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_MAINTAIN_REF_DATA"),
            scopes = listOf("write"),
            user = "bobby.beans",
          ),
        )
        .body(BodyInserters.fromValue(insertDto))
        .exchange()
        .expectStatus().isCreated
        .expectBody().json("inserted_prison".loadJson())

      verify(auditService).sendAuditEvent(
        eq("PRISON_REGISTER_INSERT"),
        eq(insertDto),
        any(),
      )
      await untilCallTo { testQueueEventMessageCount() } matches { it == 1 }

      val requestJson = testSqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testQueueUrl).build()).get().messages()[0].body()
      val (message, messageId, messageAttributes) = objectMapper.readValue(requestJson, HMPPSMessage::class.java)
      assertThat(messageAttributes.eventType.Value).isEqualTo("register.prison.inserted")

      val (eventType, additionalInformation) = objectMapper.readValue(message, HMPPSDomainEvent::class.java)
      assertThat(eventType).isEqualTo("register.prison.inserted")
      assertThat(additionalInformation.prisonId).isEqualTo("MDI")
      assertThat(message.contains("A prison has been inserted"))
      verify(telemetryClient).trackEvent(eq("prison-register-insert"), any(), isNull())
    }

    @Test
    fun `insert a prison with maintain ref data role`() {
      val prison = Prison("MDI", "Inserted Prison", female = true, active = false)
      val prisonTypes = mutableSetOf(PrisonType(prison = prison, type = Type.YOI))
      prison.prisonTypes = prisonTypes
      val address = Address(
        id = 21,
        addressLine1 = "Bawtry Road",
        addressLine2 = "Hatfield Woodhouse",
        town = "Doncaster",
        county = "South Yorkshire",
        postcode = "DN7 6BW",
        country = "England",
        prison = prison,
      )
      prison.addresses = listOf(address)
      whenever(prisonRepository.findById("MDI")).thenReturn(Optional.empty(), Optional.of(prison))
      whenever(prisonRepository.save(any())).thenReturn(prison)

      val insertDto = InsertPrisonDto(
        "MDI",
        "Inserted Prison",
        female = true,
        male = false,
        active = false,
        contracted = false,
        prisonTypes = setOf(Type.YOI),
        addresses = listOf(
          UpdateAddressDto(
            "Bawtry Road",
            "Hatfield Woodhouse",
            "Doncaster",
            "South Yorkshire",
            "DN7 6BW",
            "England",
          ),
        ),
      )

      webTestClient.post()
        .uri("/prison-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_MAINTAIN_REF_DATA"),
            scopes = listOf("write"),
            user = "bobby.beans",
          ),
        )
        .body(BodyInserters.fromValue(insertDto))
        .exchange()
        .expectStatus().isCreated
        .expectBody().json("inserted_prison_with_address".loadJson())

      verify(auditService).sendAuditEvent(
        eq("PRISON_REGISTER_INSERT"),
        eq(insertDto),
        any(),
      )
      await untilCallTo { testQueueEventMessageCount() } matches { it == 1 }

      val requestJson = testSqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testQueueUrl).build()).get().messages()[0].body()
      val (message, messageId, messageAttributes) = objectMapper.readValue(requestJson, HMPPSMessage::class.java)
      assertThat(messageAttributes.eventType.Value).isEqualTo("register.prison.inserted")

      val (eventType, additionalInformation) = objectMapper.readValue(message, HMPPSDomainEvent::class.java)
      assertThat(eventType).isEqualTo("register.prison.inserted")
      assertThat(additionalInformation.prisonId).isEqualTo("MDI")
      assertThat(message.contains("A prison has been inserted"))
      verify(telemetryClient).trackEvent(eq("prison-register-insert"), any(), isNull())
    }

    @Test
    fun `insert a prison with maintain prison data roles`() {
      val prison = Prison("MDI", "Inserted Prison", female = true, active = false)
      val prisonTypes = mutableSetOf(PrisonType(prison = prison, type = Type.YOI))
      prison.prisonTypes = prisonTypes
      val address = Address(
        id = 21,
        addressLine1 = "Bawtry Road",
        addressLine2 = "Hatfield Woodhouse",
        town = "Doncaster",
        county = "South Yorkshire",
        postcode = "DN7 6BW",
        country = "England",
        prison = prison,
      )
      prison.addresses = listOf(address)
      whenever(prisonRepository.findById("MDI")).thenReturn(Optional.empty(), Optional.of(prison))
      whenever(prisonRepository.save(any())).thenReturn(prison)

      val insertDto = InsertPrisonDto(
        "MDI",
        "Inserted Prison",
        female = true,
        male = false,
        active = false,
        contracted = false,
        prisonTypes = setOf(Type.YOI),
        addresses = listOf(
          UpdateAddressDto(
            "Bawtry Road",
            "Hatfield Woodhouse",
            "Doncaster",
            "South Yorkshire",
            "DN7 6BW",
            "England",
          ),
        ),
      )

      webTestClient.post()
        .uri("/prison-maintenance")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_MAINTAIN_PRISON_DATA"),
            scopes = listOf("write"),
            user = "bobby.beans",
          ),
        )
        .body(BodyInserters.fromValue(insertDto))
        .exchange()
        .expectStatus().isCreated
        .expectBody().json("inserted_prison_with_address".loadJson())

      verify(auditService).sendAuditEvent(
        eq("PRISON_REGISTER_INSERT"),
        eq(insertDto),
        any(),
      )
      await untilCallTo { testQueueEventMessageCount() } matches { it == 1 }

      val requestJson = testSqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testQueueUrl).build()).get().messages()[0].body()
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
