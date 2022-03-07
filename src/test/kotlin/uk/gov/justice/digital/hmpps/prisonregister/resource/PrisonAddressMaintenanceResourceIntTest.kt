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
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.AddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.HMPPSDomainEvent
import java.util.Optional

class PrisonAddressMaintenanceResourceIntTest(@Autowired private val objectMapper: ObjectMapper) : IntegrationTest() {

  @MockBean
  private lateinit var addressRepository: AddressRepository

  @MockBean
  private lateinit var auditService: AuditService

  @MockBean
  private lateinit var telemetryClient: TelemetryClient

  @Nested
  inner class UpdatePrisonAddress {

    @Test
    fun `requires a valid authentication token`() {
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI/address/21")
        .accept(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            UpdateAddressDto(
              "line1", "line2", "town", "county",
              "postcode", "country"
            )
          )
        )
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `correct permission is needed to update prison address data`() {
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI/address/21")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY"), scopes = listOf("write")))
        .body(
          BodyInserters.fromValue(
            UpdateAddressDto(
              "line1", "line2", "town", "county",
              "postcode", "country"
            )
          )
        ).exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `correct scope is needed to update prison address data`() {
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI/address/21")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("read")))
        .body(
          BodyInserters.fromValue(
            UpdateAddressDto(
              "line1", "line2", "town", "county",
              "postcode", "country"
            )
          )
        )
        .exchange().expectStatus().isForbidden
    }

    @Test
    fun `update a prison address with bad data`() {
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI/address/21")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "town" to "town",
              "country" to "country"
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
      verifyNoInteractions(auditService)
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `update a prison address`() {
      val prison = Prison("MDI", "A Prison", active = true)
      val address = Address(
        id = 21,
        addressLine1 = "Bawtry Road",
        addressLine2 = "Hatfield Woodhouse",
        town = "Doncaster",
        county = "South Yorkshire",
        country = "England",
        postcode = "DN7 6BW",
        prison = prison
      )
      prison.addresses = listOf(address)

      whenever(addressRepository.findById(any())).thenReturn(
        Optional.of(address)
      )
      webTestClient.put()
        .uri("/prison-maintenance/id/MDI/address/21")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_MAINTAIN_REF_DATA"),
            scopes = listOf("write"),
            user = "bobby.beans"
          )
        )
        .body(
          BodyInserters.fromValue(
            UpdateAddressDto(
              "first line", "second line", "Sheffield", "South Yorkshire",
              "S1 2AB", "England"
            )
          )
        )
        .exchange()
        .expectStatus().isOk
        .expectBody().json("updated_prison_address".loadJson())

      verify(auditService).sendAuditEvent(
        eq("PRISON_REGISTER_ADDRESS_UPDATE"),
        eq(
          mapOf(
            Pair("prisonId", "MDI"),
            Pair(
              "address",
              AddressDto(
                21, "first line", "second line", "Sheffield", "South Yorkshire",
                "S1 2AB", "England"
              )
            )
          )
        ),
        any()
      )

      await untilCallTo { testQueueEventMessageCount() } matches { it == 1 }

      val requestJson = testSqsClient.receiveMessage(testQueueUrl).messages[0].body
      val (message, messageId, messageAttributes) = objectMapper.readValue(requestJson, HMPPSMessage::class.java)
      assertThat(messageAttributes.eventType.Value).isEqualTo("register.prison.amended")

      val (eventType, additionalInformation) = objectMapper.readValue(message, HMPPSDomainEvent::class.java)
      assertThat(eventType).isEqualTo("register.prison.amended")
      assertThat(additionalInformation.prisonId).isEqualTo("MDI")
      assertThat(message.contains("A prison has been updated"))
      verify(telemetryClient).trackEvent(eq("prison-register-address-update"), any(), isNull())
    }
  }

  private fun String.loadJson(): String =
    PrisonAddressMaintenanceResourceIntTest::class.java.getResource("$this.json")?.readText()
      ?: throw AssertionError("file $this.json not found")
}
