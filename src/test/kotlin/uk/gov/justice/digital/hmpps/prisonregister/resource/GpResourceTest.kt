@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonGpPractice
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import java.util.Optional

class GpResourceTest : IntegrationTest() {
  @MockBean
  private lateinit var prisonRepository: PrisonRepository

  @Test
  fun `find by id prison`() {
    val prison = Prison("MDI", "Moorland (HMP & YOI)", true)
    prison.gpPractice = PrisonGpPractice("MDI", "Y05537")
    whenever(prisonRepository.findById(anyString())).thenReturn(
      Optional.of(prison)
    )
    webTestClient.get().uri("/gp/prison/MDI")
      .exchange()
      .expectStatus().isOk
      .expectBody().json("prison_id_mdi_with_gp".loadJson())
  }

  @Test
  fun `find by id prison case insensitive match`() {
    val prison = Prison("MDI", "Moorland (HMP & YOI)", true)
    prison.gpPractice = PrisonGpPractice("MDI", "Y05537")
    whenever(prisonRepository.findById(anyString())).thenReturn(
      Optional.of(prison)
    )
    webTestClient.get().uri("/gp/prison/mdi")
      .exchange()
      .expectStatus().isOk
      .expectBody().json("prison_id_mdi_with_gp".loadJson())
  }

  @Test
  fun `find by id prison no gp practice mapped`() {
    val prison = Prison("MDI", "Moorland (HMP & YOI)", true)
    whenever(prisonRepository.findById(anyString())).thenReturn(
      Optional.of(prison)
    )
    webTestClient.get().uri("/gp/prison/MDI")
      .exchange()
      .expectStatus().isOk
      .expectBody().json("prison_id_mdi_no_gp".loadJson())
  }

  @Test
  fun `find by id prison validation failure`() {
    webTestClient.get().uri("/gp/prison/1234561234561")
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().json("prison_id_badrequest_getPrisonFromId".loadJson())
  }

  @Test
  fun `find by id not found`() {
    webTestClient.get().uri("/gp/prison/123456")
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `find by gp practice prison`() {
    val prison = Prison("MDI", "Moorland (HMP & YOI)", true)
    prison.gpPractice = PrisonGpPractice("MDI", "Y05537")
    whenever(prisonRepository.findByGpPracticeGpPracticeCode(anyString())).thenReturn(prison)
    webTestClient.get().uri("/gp/practice/Y05537")
      .exchange()
      .expectStatus().isOk
      .expectBody().json("prison_id_mdi_with_gp".loadJson())
  }

  @Test
  fun `find by gp practice prison case insensitive match`() {
    val prison = Prison("MDI", "Moorland (HMP & YOI)", true)
    prison.gpPractice = PrisonGpPractice("MDI", "Y05537")
    whenever(prisonRepository.findByGpPracticeGpPracticeCode(anyString())).thenReturn(prison)
    webTestClient.get().uri("/gp/practice/y05537")
      .exchange()
      .expectStatus().isOk
      .expectBody().json("prison_id_mdi_with_gp".loadJson())
  }

  @Test
  fun `find by gp practice find prison no gp practice mapped`() {
    val prison = Prison("MDI", "Moorland (HMP & YOI)", true)
    whenever(prisonRepository.findByGpPracticeGpPracticeCode(anyString())).thenReturn(prison)
    webTestClient.get().uri("/gp/practice/Y05537")
      .exchange()
      .expectStatus().isOk
      .expectBody().json("prison_id_mdi_no_gp".loadJson())
  }

  @Test
  fun `find by gp practice not found`() {
    webTestClient.get().uri("/gp/practice/Y05537")
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `find by gp practice find prison validation failure`() {
    webTestClient.get().uri("/gp/practice/1234567")
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().json("prison_id_badrequest_getPrisonFromGpPrescriber".loadJson())
  }

  private fun String.loadJson(): String =
    GpResourceTest::class.java.getResource("$this.json").readText()
}
