package uk.gov.justice.digital.hmpps.courtregister.resource

import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.courtregister.jpa.Court
import uk.gov.justice.digital.hmpps.courtregister.jpa.CourtRepository
import java.util.Optional

class CourtResourceTest : IntegrationTest() {
  @MockBean
  private lateinit var courtRepository: CourtRepository

  @Suppress("ClassName")
  @Nested
  inner class findAll {
    @Test
    fun `find courts`() {
      val courts = listOf(
        Court("ACCRYC", "Accrington Youth Court", null, true),
        Court("KIDDYC", "Kidderminster Youth Court", null, true)
      )

      whenever(courtRepository.findAll()).thenReturn(
        courts
      )
      webTestClient.get().uri("/courts")
        .exchange()
        .expectStatus().isOk
        .expectBody().json("courts".loadJson())
    }
  }

  @Suppress("ClassName")
  @Nested
  inner class findById {
    @Test
    fun `find court`() {
      val court = Court("ACCRYC", "Accrington Youth Court", null, true)

      whenever(courtRepository.findById(anyString())).thenReturn(
        Optional.of(court)
      )
      webTestClient.get().uri("/courts/id/ACCRYC")
        .exchange()
        .expectStatus().isOk
        .expectBody().json("court_id_ACCRYC".loadJson())
    }

    @Test
    fun `find court validation failure`() {
      webTestClient.get().uri("/courts/id/1234567890123")
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().json("court_id_badrequest_getCourtFromId".loadJson())
    }
  }

  private fun String.loadJson(): String =
    CourtResourceTest::class.java.getResource("$this.json").readText()
}
