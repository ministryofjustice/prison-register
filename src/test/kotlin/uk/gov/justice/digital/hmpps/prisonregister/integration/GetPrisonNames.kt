package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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

  @Test
  fun `should return prison names active only and ids by name order`() {
    // Given
    val endPoint = "/prisons/names?active=true"

    // When
    val responseSpec = webTestClient.get().uri(endPoint)
      .exchange()

    // Then
    responseSpec.expectStatus().isOk

    val prisonNames = getPrisonNames(responseSpec.expectBody())
    Assertions.assertThat(prisonNames).isNotEmpty
    with(prisonNames[0]) {
      Assertions.assertThat(prisonId).isEqualTo("ACI")
      Assertions.assertThat(prisonName).isEqualTo("Altcourse (HMP & YOI)")
    }
    with(prisonNames.last()) {
      Assertions.assertThat(prisonId).isEqualTo("WMI")
      Assertions.assertThat(prisonName).isEqualTo("Wymott (HMP & YOI)")
    }
  }

  @Test
  fun `should return prison non active names only and ids by name order`() {
    // Given
    val endPoint = "/prisons/names?active=false"

    // When
    val responseSpec = webTestClient.get().uri(endPoint)
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
      Assertions.assertThat(prisonId).isEqualTo("WOI")
      Assertions.assertThat(prisonName).isEqualTo("Wolds (HMP)")
    }
  }

  /**
   * Different combinations of 'active' and 'name' for positive (is not empty result) and negative (is empty result) scenarios,
   * CVS(active,prisonId,isNotEmpty)
   *
   * WDI : active prisonID with name "Wakefield (HMP)"
   * WOI : non active prisonID with name "Wolds (HMP)"
   */
  @ParameterizedTest
  @CsvSource(
    "true,WDI,true",
    "null,WDI,true",
    "false,WOI,true",
    "null,WOI,true",
    "false,WDI,false",
    "true,WOI,false",
    "false,XXX,false",
    "true,XXX,false",
    "null,XXX,false",
  )
  fun `should return prison names based on any kind of active value, name, positive scenarios`(active: String?, prison_id: String?, isNotEmpty: Boolean) {
    val queryParams = mutableListOf<String>()
    if (active != "null") {
      queryParams.add("active=$active")
    }
    if (prison_id != "null") {
      queryParams.add("prison_id=$prison_id")
    }

    val endPoint = "/prisons/names?" + queryParams.joinToString("&")

    // When
    val responseSpec = webTestClient.get().uri(endPoint)
      .exchange()

    // Then
    responseSpec.expectStatus().isOk

    val prisonNames = getPrisonNames(responseSpec.expectBody())

    when (isNotEmpty) {
      true -> {
        Assertions.assertThat(prisonNames).isNotEmpty
        when (prison_id) {
          "WDI" -> with(prisonNames.last()) {
            Assertions.assertThat(prisonId).isEqualTo("WDI")
            Assertions.assertThat(prisonName).isEqualTo("Wakefield (HMP)")
          }
          "WOI" -> with(prisonNames.last()) {
            Assertions.assertThat(prisonId).isEqualTo("WOI")
            Assertions.assertThat(prisonName).isEqualTo("Wolds (HMP)")
          }
        }
      }
      false -> Assertions.assertThat(prisonNames).isEmpty()
    }
  }

  fun getPrisonNames(returnResult: WebTestClient.BodyContentSpec): Array<PrisonNameDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<PrisonNameDto>::class.java)
  }
}
