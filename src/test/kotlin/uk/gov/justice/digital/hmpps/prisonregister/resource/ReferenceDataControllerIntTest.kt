package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

class ReferenceDataControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class Security {
    @ParameterizedTest
    @ValueSource(strings = ["/reference-data/suggestions/counties", "/reference-data/suggestions/towns"])
    fun `requires a valid authentication token`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @ParameterizedTest
    @ValueSource(strings = ["/reference-data/suggestions/counties", "/reference-data/suggestions/towns"])
    fun `requires correct role`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @ValueSource(strings = ["/reference-data/suggestions/counties", "/reference-data/suggestions/towns"])
    fun `allowed with correct role`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @DisplayName("GET /reference-data/suggestions/counties")
  @Nested
  inner class GetCountySuggestions {
    @Test
    fun `will return all county suggestions`() {
      val counties: List<CodeDescription> = webTestClient.get()
        .uri("/reference-data/suggestions/counties")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBodyList(CodeDescription::class.java)
        .returnResult().responseBody!!

      assertThat(counties).hasSizeGreaterThan(10)
      assertThat(counties.map { it.description }).contains("South Yorkshire")
      assertThat(counties).contains(CodeDescription(code = "S.YORKSHIRE", description = "South Yorkshire"))
    }
  }

  @DisplayName("GET /reference-data/suggestions/towns")
  @Nested
  inner class GetTownSuggestions {
    @Test
    fun `will return all town suggestions`() {
      val towns: List<CodeDescription> = webTestClient.get()
        .uri("/reference-data/suggestions/towns")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBodyList(CodeDescription::class.java)
        .returnResult().responseBody!!

      assertThat(towns).hasSizeGreaterThan(100)
      assertThat(towns.map { it.description }).contains("Sheffield", "Chelmsford", "Colchester")
      assertThat(towns).contains(CodeDescription(code = "25343", description = "Sheffield"))
    }
  }
}
