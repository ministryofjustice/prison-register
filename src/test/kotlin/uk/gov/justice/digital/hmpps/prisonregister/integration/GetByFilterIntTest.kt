package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.mock.mockito.SpyBean
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto

class GetByFilterIntTest : IntegrationTest() {

  @SpyBean
  lateinit var prisonRepository: PrisonRepository

  @BeforeEach
  fun setupHighSecurityPrisons() {
    val highSecurity1 = Prison(
      "HS1",
      "High Security One",
      active = true,
      lthse = true,
    )
    val highSecurity2 = Prison(
      "HS2",
      "High Security Two",
      active = true,
      lthse = true,
    )
    val highSecurity3 = Prison(
      "HS3",
      "High Security Three",
      active = false,
      lthse = true,
    )

    prisonRepository.saveAllAndFlush(
      setOf(highSecurity1, highSecurity2, highSecurity3),
    )
  }

  @Test
  fun `When we search for prison by lthse, only lthse prisons are returned`() {
    val endpoint = "/prisons/search?lthse=true"
    val searchResults = getSearchResults(endpoint)

    assertThat(searchResults).size().isEqualTo(3)

    searchResults.forEach {
      assertThat(it.lthse).isTrue()
    }
  }

  @Test
  fun `When we search for prison by active and lthse, only prisons that are both active and lthse are returned`() {
    val endpoint = "/prisons/search?lthse=true&active=true"
    val searchResults = getSearchResults(endpoint)

    assertThat(searchResults).size().isEqualTo(2)

    searchResults.forEach {
      assertThat(it.lthse).isTrue()
      assertThat(it.active).isTrue()
    }
  }

  @Test
  fun `When we search for prison by not active and lthse, only prisons that are both not active and lthse are returned`() {
    val endpoint = "/prisons/search?lthse=true&active=false"
    val searchResults = getSearchResults(endpoint)

    assertThat(searchResults).size().isEqualTo(1)

    searchResults.forEach {
      assertThat(it.lthse).isTrue()
      assertThat(it.active).isFalse()
    }
  }

  private fun getSearchResults(endpoint: String): List<PrisonDto> {
    val headers = createAnyRole()

    val responseSpec = webTestClient.get()
      .uri(endpoint)
      .headers(headers)
      .exchange()

    responseSpec.expectStatus()
      .isOk

    return responseSpec.expectBodyList(PrisonDto::class.java)
      .returnResult().responseBody!!
  }
}
