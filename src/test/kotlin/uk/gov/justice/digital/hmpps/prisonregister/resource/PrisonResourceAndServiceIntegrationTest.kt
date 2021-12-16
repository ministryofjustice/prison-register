package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import java.util.Optional

class PrisonResourceAndServiceIntegrationTest : IntegrationTest() {
  @MockBean
  private lateinit var prisonRepository: PrisonRepository

  @Suppress("ClassName")
  @Nested
  inner class findAll {
    @Test
    fun `find prisons`() {
      val prisons = listOf(
        Prison("MDI", "Moorland HMP", true),
        Prison("LEI", "Leeds HMP", true)
      )

      whenever(prisonRepository.findAll()).thenReturn(
        prisons
      )
      webTestClient.get().uri("/prisons")
        .exchange()
        .expectStatus().isOk
        .expectBody().json("prisons".loadJson())
    }
  }

  @Suppress("ClassName")
  @Nested
  inner class findById {
    @Test
    fun `find prison`() {
      val prison = Prison("MDI", "Moorland HMP", true)

      whenever(prisonRepository.findById(anyString())).thenReturn(
        Optional.of(prison)
      )
      webTestClient.get().uri("/prisons/id/MDI")
        .exchange()
        .expectStatus().isOk
        .expectBody().json("prison_id_MDI".loadJson())
    }

    @Test
    fun `find prison validation failure`() {
      webTestClient.get().uri("/prisons/id/1234567890123")
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().json("prison_id_badrequest_getPrisonFromId".loadJson())
    }
  }

  private fun String.loadJson(): String =
    PrisonResourceAndServiceIntegrationTest::class.java.getResource("$this.json").readText()
}
