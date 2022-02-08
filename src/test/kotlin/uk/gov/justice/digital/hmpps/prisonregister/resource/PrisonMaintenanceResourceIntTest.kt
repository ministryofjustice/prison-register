package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.UpdatePrisonDto
import java.util.Optional

class PrisonMaintenanceResourceIntTest : IntegrationTest() {

  @MockBean
  private lateinit var prisonRepository: PrisonRepository

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
    }
  }

  private fun String.loadJson(): String =
    PrisonMaintenanceResourceIntTest::class.java.getResource("$this.json")?.readText()
      ?: throw AssertionError("file $this.json not found")
}
