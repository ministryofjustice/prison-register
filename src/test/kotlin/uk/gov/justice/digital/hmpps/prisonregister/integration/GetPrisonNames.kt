package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.PrisonNameDto

class GetPrisonNames : IntegrationTest() {
  @Test
  fun `should return prison names and ids by name order`() {
    // Given
    val endPont = "/prisons/names"

    // When
    val responseSpec = webTestClient.get().uri(endPont)
      .exchange()

    // Then
    responseSpec.expectStatus().isOk

    val prisonNames = getPrisonNames(responseSpec.expectBody())
    Assertions.assertThat(prisonNames).isNotEmpty
    with(prisonNames[0]) {
      Assertions.assertThat(prisonId).isEqualTo("AKI")
      Assertions.assertThat(prisonName).isEqualTo("Acklington (HMP)")
    }
    with(prisonNames.last()) {
      Assertions.assertThat(prisonId).isEqualTo("WMI")
      Assertions.assertThat(prisonName).isEqualTo("Wymott (HMP & YOI)")
    }
  }

  fun getPrisonNames(returnResult: WebTestClient.BodyContentSpec): Array<PrisonNameDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<PrisonNameDto>::class.java)
  }
}
