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
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.HMPPSDomainEvent
import java.util.Optional

class PrisonAddressMaintenanceResourceIntTest(@Autowired private val objectMapper: ObjectMapper) : IntegrationTest() {

  @MockBean
  private lateinit var addressRepository: AddressRepository

  @MockBean
  private lateinit var prisonRepository: PrisonRepository

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

  @Nested
  inner class DeletePrisonAddress {
    @Test
    fun `requires a valid authentication token`() {
      webTestClient.delete()
        .uri("/prison-maintenance/id/MDI/address/1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `correct permission is needed to delete an address for an existing prison`() {
      webTestClient.delete()
        .uri("/prison-maintenance/id/MDI/address/1")
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY"), scopes = listOf("write")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `correct scope is needed to delete an address for an existing prison`() {
      webTestClient.delete()
        .uri("/prison-maintenance/id/MDI/address/21")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("read")))
        .exchange().expectStatus().isForbidden
    }

    @Test
    fun `delete an address not found`() {
      whenever(addressRepository.findById(21L)).thenReturn(Optional.empty())

      webTestClient.delete()
        .uri("/prison-maintenance/id/MDI/address/21")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_MAINTAIN_REF_DATA"),
            scopes = listOf("write"),
            user = "bobby.beans"
          )
        )
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `delete an address for an existing prison`() {
      val prison = Prison("MDI", "A Prison", active = true)
      val address = Address(
        id = 21L,
        addressLine1 = "first line",
        addressLine2 = "second line",
        town = "Sheffield",
        county = "South Yorkshire",
        country = "England",
        postcode = "S1 2AB",
        prison = prison
      )

      whenever(addressRepository.findById(address.id as Long)).thenReturn(Optional.of(address))

      webTestClient.delete()
        .uri("/prison-maintenance/id/MDI/address/21")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_MAINTAIN_REF_DATA"),
            scopes = listOf("write"),
            user = "bobby.beans"
          )
        )
        .exchange()
        .expectStatus().isOk

      verify(auditService).sendAuditEvent(
        eq("PRISON_REGISTER_ADDRESS_DELETE"),
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
      verify(telemetryClient).trackEvent(eq("prison-register-address-delete"), any(), isNull())
    }
  }

  @Nested
  inner class AddPrisonAddress {
    @Test
    fun `requires a valid authentication token`() {
      webTestClient.post()
        .uri("/prison-maintenance/id/MDI/address")
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
    fun `correct permission is needed to add a new address to an existing prison`() {
      webTestClient.post()
        .uri("/prison-maintenance/id/MDI/address")
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
    fun `correct scope is needed to add a new address to an existing prison`() {
      webTestClient.post()
        .uri("/prison-maintenance/id/MDI/address")
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
    fun `add a new prison address with bad data`() {
      webTestClient.post()
        .uri("/prison-maintenance/id/MDI/address")
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
      verifyNoInteractions(auditService, telemetryClient, prisonRepository, addressRepository)
    }

    @Test
    fun `add a new address to an existing prison`() {
      val prison = Prison("MDI", "A Prison", active = true)

      whenever(prisonRepository.findById(prison.prisonId)).thenReturn(Optional.of(prison))

      val additionalAddressDetails = UpdateAddressDto(
        "first line", "second line", "Sheffield", "South Yorkshire",
        "S1 2AB", "England"
      )

      with(additionalAddressDetails) {
        val address = Address(
          addressLine1 = addressLine1,
          addressLine2 = addressLine2,
          town = town,
          county = county,
          country = country,
          postcode = postcode,
          prison = prison
        )

        val savedAddress = Address(
          id = 21L,
          addressLine1 = addressLine1,
          addressLine2 = addressLine2,
          town = town,
          county = county,
          country = country,
          postcode = postcode,
          prison = prison
        )

        whenever(addressRepository.save(address)).thenReturn(savedAddress)
      }

      webTestClient.post()
        .uri("/prison-maintenance/id/MDI/address")
        .accept(MediaType.APPLICATION_JSON)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_MAINTAIN_REF_DATA"),
            scopes = listOf("write"),
            user = "bobby.beans"
          )
        )
        .body(BodyInserters.fromValue(additionalAddressDetails))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("updated_prison_address".loadJson())

      verify(auditService).sendAuditEvent(
        eq("PRISON_REGISTER_ADDRESS_INSERT"),
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
      verify(telemetryClient).trackEvent(eq("prison-register-address-add"), any(), isNull())
    }
  }

  private fun String.loadJson(): String =
    PrisonAddressMaintenanceResourceIntTest::class.java.getResource("$this.json")?.readText()
      ?: throw AssertionError("file $this.json not found")
}
