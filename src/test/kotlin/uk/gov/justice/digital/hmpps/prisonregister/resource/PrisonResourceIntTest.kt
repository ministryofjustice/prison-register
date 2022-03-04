package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import java.util.Optional

class PrisonResourceIntTest : IntegrationTest() {
  @MockBean
  private lateinit var prisonRepository: PrisonRepository

  @Suppress("ClassName")
  @Nested
  inner class findAll {
    @Test
    fun `find prisons`() {
      val prison = Prison("MDI", "Moorland HMP", active = true)
      val address = Address(
        addressLine1 = "Bawtry Road",
        addressLine2 = "Hatfield Woodhouse",
        town = "Doncaster",
        county = "South Yorkshire",
        country = "England",
        postcode = "DN7 6BW",
        prison = prison
      )
      prison.addresses = listOf(address)

      val prisons = listOf(
        prison,
        Prison("LEI", "Leeds HMP", active = true)
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
      val prison = Prison("MDI", "Moorland HMP", active = true)
      val mdiAddress = Address(
        addressLine1 = "Bawtry Road",
        addressLine2 = "Hatfield Woodhouse",
        town = "Doncaster",
        county = "South Yorkshire",
        country = "England",
        postcode = "DN7 6BW",
        prison = prison
      )
      prison.addresses = listOf(mdiAddress)

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

  @Suppress("ClassName")
  @Nested
  inner class getPrisonsBySearchFilter {
    @Test
    fun `search by active , text , male and female flags`() {
      val prisons = listOf(
        Prison("MDI", "Moorland HMP", active = true, male = true, female = true)
      )
      whenever(prisonRepository.findAll(any())).thenReturn(prisons)

      webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/prisons/search")
            .queryParam("active", true)
            .queryParam("textSearch", "MDI")
            .queryParam("male", true)
            .queryParam("female", true)
            .build()
        }
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].prisonId").isEqualTo("MDI")
        .jsonPath("$[0].prisonName").isEqualTo("Moorland HMP")
        .jsonPath("$[0].active").isEqualTo(true)
        .jsonPath("$[0].male").isEqualTo(true)
        .jsonPath("$[0].female").isEqualTo(true)
    }

    @Test
    fun `no search params provided`() {
      val prisons = listOf(
        Prison("MDI", "Moorland HMP", active = true)
      )
      whenever(prisonRepository.findAll(any())).thenReturn(prisons)

      webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path("/prisons/search")
            .build()
        }
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].prisonId").isEqualTo("MDI")
        .jsonPath("$[0].prisonName").isEqualTo("Moorland HMP")
        .jsonPath("$[0].active").isEqualTo(true)
    }
  }

  private fun String.loadJson(): String =
    PrisonResourceIntTest::class.java.getResource("$this.json").readText()
}
